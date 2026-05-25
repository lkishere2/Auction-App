package com.auction.app.domains.auction.bids;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.auction.notification.AuctionPublisher;
import com.auction.app.domains.auction.bids.dtos.BidNotificationPayload;
import com.auction.app.domains.auction.bids.dtos.BidRequest;
import com.auction.app.domains.auction.bids.dtos.BidResponse;
import com.auction.app.domains.auction.bids.dtos.PendingBid;
import com.auction.app.domains.auction.exceptions.AuctionNotFoundException;
import com.auction.app.domains.auction.exceptions.BidNotFoundException;
import com.auction.app.domains.auction.exceptions.InsufficientBalanceException;
import com.auction.app.domains.auction.exceptions.InvalidBidException;
import com.auction.app.domains.users.exceptions.UserNotFoundException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.app.domains.auction.auction.Auction;
import com.auction.app.domains.auction.auction.redis.AuctionCacheAdapter;
import com.auction.app.domains.auction.auction.AuctionRepository;
import com.auction.app.domains.auction.auction.AuctionStatus;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final AuctionCacheAdapter auctionCacheAdapter;
    private final AuctionPublisher publisher;

    private static final long SNIPER_PROTECTION_SECONDS = 120L;
    private static final BigDecimal INCREMENT_PERCENTAGE = BigDecimal.valueOf(0.05);

    @Transactional
    public void placeBid(Long auctionId, BidRequest request, Principal principal) {
        if (principal == null) {
            throw new InvalidBidException("Authentication required to place a bid.");
        }

        // Fetch some info
        User bidder = (User) ((Authentication) principal).getPrincipal();
        AuctionResponse response = getActiveAuctionResponse(auctionId);
        String sellerLabel = response.getSellerLabel();
        Long sellerId = Long.valueOf(sellerLabel.substring(sellerLabel.lastIndexOf("#") + 1));

        // Validate
        validateUser(bidder.getId(), sellerId);
        validateBidAmount(request.getAmount(), response);
        validateSpendableBalance(bidder, request.getAmount());

        // Find auction
        Auction auction = findAuctionById(auctionId);

        // Persist PENDING bid immediately — funds are locked from this point
        Bid pendingBid = buildBid(auction, bidder, request.getAmount());
        Bid saved = bidRepository.save(pendingBid);

        // Enqueue to Redis for processing
        PendingBid queued = buildPendingBid(saved, auctionId, bidder, request.getAmount());
        auctionCacheAdapter.enqueueBid(auctionId, queued);
        log.info("Bid queued as PENDING — auction #{}, bidder #{}, amount ${}",
                auctionId, bidder.getId(), request.getAmount());

        // Process immediately and broadcast to all subscribers
        processNextBid(auctionId).ifPresent(publisher::publish);
    }

    @Transactional
    public Optional<BidNotificationPayload> processNextBid(Long auctionId) {

        PendingBid pendingBid = auctionCacheAdapter.dequeueBid(auctionId);
        if (pendingBid == null) return Optional.empty();

        Bid bid = findBidById(pendingBid.getBidId());
        AuctionResponse response = auctionCacheAdapter.getAuctionResponse(auctionId);

        // Auction no longer active or bid amount no longer meets minimum — REFUND
        if (!isBidEligible(response, pendingBid.getAmount())) {
            bid.setStatus(BidStatus.REFUNDED);
            bidRepository.save(bid);
            log.info("Bid #{} rejected after dequeue — marked REFUNDED. auction #{}", bid.getId(), auctionId);
            return Optional.empty();
        }

        User bidder = findBidderById(pendingBid.getBidderId());
        // Re-check spendable balance at processing time — REFUND if insufficient
        if (!hasSufficientBalance(bidder, pendingBid.getAmount())) {
            bid.setStatus(BidStatus.REFUNDED);
            bidRepository.save(bid);
            log.info("Bid #{} rejected after dequeue — insufficient balance. bidder #{}", bid.getId(), pendingBid.getBidderId());
            return Optional.empty();
        }

        // Outbid the previous highest bidder — flip their HELD to REFUNDED
        refundPreviousHighestBidder(auctionId);

        // Promote this bid from PENDING to HELD
        bid.setStatus(BidStatus.HELD);
        bidRepository.save(bid);

        boolean isExtended = applySniperProtection(response);
        BigDecimal newIncrement = calculateIncrement(pendingBid.getAmount());

        response.setCurrentPrice(pendingBid.getAmount());
        response.setMinBidIncrement(newIncrement);
        response.setBidCount(response.getBidCount() + 1);
        response.setWinnerLabel(pendingBid.getBidderLabel());
        auctionCacheAdapter.updateAuctionResponse(auctionId, response);

        log.info("Bid #{} promoted to HELD — auction #{}, price ${}, bidder #{}",
                bid.getId(), auctionId, pendingBid.getAmount(), pendingBid.getBidderId());

        return Optional.of(BidNotificationPayload.builder()
                .auctionId(auctionId)
                .currentPrice(pendingBid.getAmount())
                .minNextBid(pendingBid.getAmount().add(newIncrement))
                .bidderLabel(pendingBid.getBidderLabel())
                .endTime(response.getEndTime())
                .extended(isExtended)
                .bidCount(response.getBidCount())
                .build());
    }

    public List<BidResponse> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByPlacedAtDesc(auctionId)
                .stream()
                .map(BidResponse::from)
                .toList();
    }

    public List<Long> getAuctionsBiddenByCurrentUser() {
        User user = currentUser();
        return bidRepository.findDistinctAuctionIdsByBidderId(user.getId());
    }

    // Helpers

    // Builders
    private Bid buildBid(Auction auction, User bidder, BigDecimal amount) {
        return Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(amount)
                .build();
    }

    private PendingBid buildPendingBid(Bid bid, Long auctionId, User bidder, BigDecimal amount) {
        return PendingBid.builder()
                .bidId(bid.getId())
                .auctionId(auctionId)
                .bidderId(bidder.getId())
                .bidderLabel(bidder.getDisplayName() + " #" + bidder.getId())
                .amount(amount)
                .placedAt(bid.getPlacedAt())
                .build();
    }

    // Finders
    private AuctionResponse getActiveAuctionResponse(Long auctionId) {
        AuctionResponse response = auctionCacheAdapter.getAuctionResponse(auctionId);
        if (response == null || response.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionNotFoundException("Auction with ID " + auctionId + " is not found or not active.");
        }
        return response;
    }

    private Auction findAuctionById(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction with ID " + auctionId + " was not found."));
    }

    private Bid findBidById(Long bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException("Bid record with ID " + bidId + " was not found."));
    }

    private User findBidderById(Long bidderId) {
        return userRepository.findById(bidderId)
                .orElseThrow(() -> new UserNotFoundException("Bidder with ID " + bidderId + " was not found."));
    }

    // Validators
    private void validateBidAmount(BigDecimal amount, AuctionResponse response) {
        BigDecimal minimumBid = response.getCurrentPrice().add(response.getMinBidIncrement());
        if (amount.compareTo(minimumBid) < 0) {
            throw new InvalidBidException("Bid must be at least " + minimumBid + " (current price + minimum increment)");
        }
    }

    private void validateSpendableBalance(User bidder, BigDecimal amount) {
        BigDecimal spendable = getSpendableBalance(bidder);
        if (amount.compareTo(spendable) > 0) {
            throw new InsufficientBalanceException("Insufficient spendable balance. Available: " + spendable + ", Attempted: " + amount);
        }
    }

    private BigDecimal getSpendableBalance(User user) {
        BigDecimal locked = bidRepository.sumLockedAmountByBidderIdAndStatuses(
                user.getId(), List.of(BidStatus.PENDING, BidStatus.HELD)
        );
        return user.getBalance().subtract(locked);
    }

    private void validateUser(Long bidderId, Long sellerId) {
        if (sellerId.equals(bidderId)) {
            throw new InvalidBidException("You cannot bid on your own auction");
        }
    }

    private boolean isBidEligible(AuctionResponse response, BigDecimal amount) {
        if (response == null || response.getStatus() != AuctionStatus.ACTIVE) return false;
        BigDecimal minimumBid = response.getCurrentPrice().add(response.getMinBidIncrement());
        return amount.compareTo(minimumBid) >= 0;
    }

    private boolean hasSufficientBalance(User bidder, BigDecimal amount) {
        return amount.compareTo(getSpendableBalance(bidder)) <= 0;
    }

    // Other
    private void refundPreviousHighestBidder(Long auctionId) {
        bidRepository.findByAuctionIdAndStatus(auctionId, BidStatus.HELD)
                .ifPresent(oldBid -> {
                    oldBid.setStatus(BidStatus.REFUNDED);
                    bidRepository.save(oldBid);
                    log.info("Bid #{} flipped to REFUNDED — outbid on auction #{}", oldBid.getId(), auctionId);
                });
    }

    private boolean applySniperProtection(AuctionResponse response) {
        Instant now = Instant.now();
        if (Duration.between(now, response.getEndTime()).getSeconds() < SNIPER_PROTECTION_SECONDS) {
            response.setEndTime(now.plusSeconds(SNIPER_PROTECTION_SECONDS));
            log.info("Auction #{} extended by 2 minutes", response.getId());
            return true;
        }
        return false;
    }

    private BigDecimal calculateIncrement(BigDecimal amount) {
        return amount.multiply(INCREMENT_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidBidException("User is not authenticated.");
        }
        return (User) authentication.getPrincipal();
    }
}
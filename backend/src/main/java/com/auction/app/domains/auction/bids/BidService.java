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

// TODO: Double check the bid service
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
        User bidder = (User) ((Authentication) principal).getPrincipal();
        AuctionResponse response = getActiveAuctionResponse(auctionId);

        // Extract sellerId from the label (Format: "DisplayName #123")
        String sellerLabel = response.getSellerLabel();
        Long sellerId = Long.valueOf(sellerLabel.substring(sellerLabel.lastIndexOf("#") + 1));

        if (sellerId.equals(bidder.getId())) {
            throw new RuntimeException("You cannot bid on your own auction");
        }

        validateBidAmount(request.getAmount(), response);
        validateSpendableBalance(bidder, request.getAmount());

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Persist PENDING bid immediately — funds are locked from this point
        Bid pendingBid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(request.getAmount())
                .status(BidStatus.PENDING)
                .placedAt(Instant.now())
                .build();
        Bid saved = bidRepository.save(pendingBid);

        // Enqueue to Redis for processing
        PendingBid queued = PendingBid.builder()
                .bidId(saved.getId())
                .auctionId(auctionId)
                .bidderId(bidder.getId())
                .bidderLabel(bidder.getDisplayName() + " #" + bidder.getId())
                .amount(request.getAmount())
                .placedAt(saved.getPlacedAt())
                .build();

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

        Bid bid = bidRepository.findById(pendingBid.getBidId())
                .orElseThrow(() -> new RuntimeException("Bid record not found"));

        AuctionResponse response = auctionCacheAdapter.getAuctionResponse(auctionId);

        // Auction no longer active or bid amount no longer meets minimum — REFUND
        if (!isBidEligible(response, pendingBid.getAmount())) {
            bid.setStatus(BidStatus.REFUNDED);
            bidRepository.save(bid);
            log.info("Bid #{} rejected after dequeue — marked REFUNDED. auction #{}", bid.getId(), auctionId);
            return Optional.empty();
        }

        User bidder = userRepository.findById(pendingBid.getBidderId())
                .orElseThrow(() -> new RuntimeException("Bidder not found"));

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return bidRepository.findDistinctAuctionIdsByBidderId(user.getId());
    }

    // Helpers
    private AuctionResponse getActiveAuctionResponse(Long auctionId) {
        AuctionResponse response = auctionCacheAdapter.getAuctionResponse(auctionId);
        if (response == null || response.getStatus() != AuctionStatus.ACTIVE) {
            throw new RuntimeException("Auction not found or not active");
        }
        return response;
    }

    private void validateBidAmount(BigDecimal amount, AuctionResponse response) {
        BigDecimal minimumBid = response.getCurrentPrice().add(response.getMinBidIncrement());
        if (amount.compareTo(minimumBid) < 0) {
            throw new RuntimeException("Bid must be at least " + minimumBid + " (current price + 5%)");
        }
    }

    private void validateSpendableBalance(User bidder, BigDecimal amount) {
        BigDecimal spendable = getSpendableBalance(bidder);
        if (amount.compareTo(spendable) > 0) {
            throw new RuntimeException("Insufficient balance. Spendable: " + spendable);
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

    private BigDecimal getSpendableBalance(User user) {
        BigDecimal locked = bidRepository.sumLockedAmountByBidderIdAndStatuses(
                user.getId(), List.of(BidStatus.PENDING, BidStatus.HELD));
        return user.getBalance().subtract(locked);
    }

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
}
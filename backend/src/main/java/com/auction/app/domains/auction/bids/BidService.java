package com.auction.app.domains.auction.bids;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.auction.app.domains.auction.bids.dtos.BidNotificationPayload;
import com.auction.app.domains.auction.bids.dtos.BidRequest;
import com.auction.app.domains.auction.bids.dtos.BidResponse;
import com.auction.app.domains.auction.bids.dtos.PendingBid;
import com.auction.app.domains.auction.bids.exceptions.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.app.domains.auction.auction.Auction;
import com.auction.app.domains.auction.auction.redis.AuctionCacheAdapter;
import com.auction.app.domains.auction.auction.AuctionRepository;
import com.auction.app.domains.auction.auction.dtos.AuctionState;
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

    private static final long SNIPER_PROTECTION_SECONDS = 120L;
    private static final BigDecimal INCREMENT_PERCENTAGE = BigDecimal.valueOf(0.05);

    @Transactional
    public void placeBid(Long auctionId, BidRequest request) {
        User bidder = getCurrentUser();
        AuctionState state = getActiveAuctionState(auctionId);

        // Use sellerId from cache — no DB hit needed
        if (state.getSellerId().equals(bidder.getId())) {
            throw new SelfBiddingException("You cannot bid on your own auction");
        }

        validateBidAmount(request.getAmount(), state);
        validateSpendableBalance(bidder, request.getAmount());

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

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
    }

    @Transactional
    public Optional<BidNotificationPayload> processNextBid(Long auctionId) {
        PendingBid pendingBid = auctionCacheAdapter.dequeueBid(auctionId);
        if (pendingBid == null) return Optional.empty();

        // Fetch the existing PENDING bid record from DB
        Bid bid = bidRepository.findById(pendingBid.getBidId())
                .orElseThrow(() -> new BidNotFoundException("Bid record not found"));

        AuctionState state = auctionCacheAdapter.getAuctionState(auctionId);

        // Auction no longer active or bid amount no longer meets minimum — REFUND
        if (!isBidEligible(state, pendingBid.getAmount())) {
            bid.setStatus(BidStatus.REFUNDED);
            bidRepository.save(bid);
            log.info("Bid #{} rejected after dequeue — marked REFUNDED. auction #{}", bid.getId(), auctionId);
            return Optional.empty();
        }

        User bidder = userRepository.findById(pendingBid.getBidderId())
                .orElseThrow(() -> new BidderNotFoundException("Bidder not found"));

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

        boolean isExtended = applySniperProtection(state);
        BigDecimal newIncrement = calculateIncrement(pendingBid.getAmount());

        state.setCurrentPrice(pendingBid.getAmount());
        state.setMinBidIncrement(newIncrement);
        state.setBidCount(state.getBidCount() + 1);
        state.setWinnerId(pendingBid.getBidderId());
        state.setWinnerLabel(pendingBid.getBidderLabel());
        auctionCacheAdapter.updateAuctionState(auctionId, state);

        log.info("Bid #{} promoted to HELD — auction #{}, price ${}, bidder #{}",
                bid.getId(), auctionId, pendingBid.getAmount(), pendingBid.getBidderId());

        return Optional.of(BidNotificationPayload.builder()
                .auctionId(auctionId)
                .currentPrice(pendingBid.getAmount())
                .minNextBid(pendingBid.getAmount().add(newIncrement))
                .bidderLabel(pendingBid.getBidderLabel())
                .endTime(state.getEndTime())
                .extended(isExtended)
                .bidCount(state.getBidCount())
                .build());
    }

    public List<BidResponse> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByPlacedAtDesc(auctionId)
                .stream()
                .map(BidResponse::from)
                .toList();
    }

    public List<Long> getAuctionsBiddenByCurrentUser() {
        return bidRepository.findDistinctAuctionIdsByBidderId(getCurrentUser().getId());
    }

    // Helpers
    private AuctionState getActiveAuctionState(Long auctionId) {
        AuctionState state = auctionCacheAdapter.getAuctionState(auctionId);
        if (state == null || state.getStatus() != AuctionStatus.ACTIVE) {
            throw new InvalidAutionStateException("Auction not found or not active");
        }
        return state;
    }

    private void validateBidAmount(BigDecimal amount, AuctionState state) {
        BigDecimal minimumBid = state.getCurrentPrice().add(state.getMinBidIncrement());
        if (amount.compareTo(minimumBid) < 0) {
            throw new InvalidBidAmountException("Bid must be at least " + minimumBid + " (current price + 5%)");
        }
    }

    private void validateSpendableBalance(User bidder, BigDecimal amount) {
        if (amount.compareTo(getSpendableBalance(bidder)) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. Spendable: " + getSpendableBalance(bidder));
        }
    }

    private boolean isBidEligible(AuctionState state, BigDecimal amount) {
        if (state == null || state.getStatus() != AuctionStatus.ACTIVE) return false;
        BigDecimal minimumBid = state.getCurrentPrice().add(state.getMinBidIncrement());
        return amount.compareTo(minimumBid) >= 0;
    }

    private boolean hasSufficientBalance(User bidder, BigDecimal amount) {
        return amount.compareTo(getSpendableBalance(bidder)) <= 0;
    }

    // PENDING + HELD are both locked funds
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

    private boolean applySniperProtection(AuctionState state) {
        Instant now = Instant.now();
        if (Duration.between(now, state.getEndTime()).getSeconds() < SNIPER_PROTECTION_SECONDS) {
            state.setEndTime(now.plusSeconds(SNIPER_PROTECTION_SECONDS));
            log.info("Auction #{} extended by 2 minutes", state.getAuctionId());
            return true;
        }
        return false;
    }

    private BigDecimal calculateIncrement(BigDecimal amount) {
        return amount.multiply(INCREMENT_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}
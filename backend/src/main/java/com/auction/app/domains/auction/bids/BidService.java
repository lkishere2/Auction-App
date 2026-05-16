package com.auction.app.domains.auction.bids;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltnc.auction.domain.auction.auc.Auction;
import com.ltnc.auction.domain.auction.auc.AuctionCacheAdapter;
import com.ltnc.auction.domain.auction.auc.AuctionRepository;
import com.ltnc.auction.domain.auction.auc.AuctionState;
import com.ltnc.auction.domain.auction.auc.AuctionStatus;
import com.auction.app.domains.auction.reservedFunds.ReservedFundStatus;
import com.auction.app.domains.auction.reservedFunds.ReservedFunds;
import com.auction.app.domains.auction.reservedFunds.ReservedFundsRepository;
import com.auction.app.domains.exceptions.AuctionNotFoundException;
import com.auction.app.domains.exceptions.InvalidAuctionStateException;
import com.auction.app.domains.exceptions.InvalidBidException;
import com.ltnc.auction.domain.user.User;
import com.ltnc.auction.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ReservedFundsRepository reservedFundsRepository;
    private final UserRepository userRepository;
    private final AuctionCacheAdapter auctionCacheAdapter;
    private final SpendableBalanceService spendableBalanceService;

    private static final long TWO_MINUTES_SECONDS = 120L;

    public void placeBid(User bidder, Long auctionId, BidRequest request) {
        AuctionState state = auctionCacheAdapter.getAuctionState(auctionId);

        if (state == null) {
            throw new AuctionNotFoundException("Auction not found or not active");
        }

        if (state.getStatus() != AuctionStatus.ACTIVE) {
            throw new InvalidAuctionStateException("Auction is not active");
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        if (auction.getSeller().getUserId().equals(bidder.getUserId())) {
            throw new InvalidBidException("You cannot bid on your own auction");
        }

        BigDecimal minimumBid = state.getCurrentPrice().add(state.getMinBidIncrement());
        if (request.amount().compareTo(minimumBid) < 0) {
            throw new InvalidBidException(
                "Bid must be at least " + minimumBid + " (current price + 5%)"
            );
        }

        BigDecimal spendable = spendableBalanceService.getSpendableBalance(bidder);
        if (request.amount().compareTo(spendable) > 0) {
            throw new InvalidBidException(
                "Insufficient balance. Spendable: " + spendable
            );
        }

        PendingBid pendingBid = PendingBid.builder()
                .auctionId(auctionId)
                .bidderId(bidder.getUserId())
                .bidderLabel(bidder.getDisplayUsername() + " #" + bidder.getUserId())
                .amount(request.amount())
                .placedAt(Instant.now())
                .build();

        auctionCacheAdapter.enqueueBid(auctionId, pendingBid);
        log.info("Bid queued — auction #{}, bidder #{}, amount ${}",
                auctionId, bidder.getUserId(), request.amount());
    }

    @Transactional
    public Optional<BidNotificationPayload> processNextBid(Long auctionId) {
        PendingBid pendingBid = auctionCacheAdapter.dequeueBid(auctionId);

        if (pendingBid == null) return Optional.empty();

        AuctionState state = auctionCacheAdapter.getAuctionState(auctionId);
        if (state == null || state.getStatus() != AuctionStatus.ACTIVE) {
            log.warn("Bid dequeued but auction #{} is no longer active", auctionId);
            return Optional.empty();
        }

        BigDecimal minimumBid = state.getCurrentPrice().add(state.getMinBidIncrement());
        if (pendingBid.getAmount().compareTo(minimumBid) < 0) {
            log.info("Bid rejected after dequeue — amount too low. auction #{}", auctionId);
            return Optional.empty();
        }

        User bidder = userRepository.findById(pendingBid.getBidderId())
                .orElseThrow(() -> new RuntimeException("Bidder not found"));

        BigDecimal spendable = spendableBalanceService.getSpendableBalance(bidder);
        if (pendingBid.getAmount().compareTo(spendable) > 0) {
            log.info("Bid rejected after dequeue — insufficient balance. bidder #{}",
                    pendingBid.getBidderId());
            return Optional.empty();
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        refundPreviousHighestBidder(auctionId, state.getWinnerId());

        reserveFunds(bidder, auction, pendingBid.getAmount());

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(pendingBid.getAmount());
        bid.setPlacedAt(pendingBid.getPlacedAt());
        bidRepository.save(bid);

        boolean extended = false;
        Instant now = Instant.now();
        long secondsLeft = Duration.between(now, state.getEndTime()).getSeconds();

        if (secondsLeft < TWO_MINUTES_SECONDS) {
            state.setEndTime(now.plusSeconds(TWO_MINUTES_SECONDS));
            extended = true;
            log.info("Auction #{} extended by 2 minutes", auctionId);
        }

        BigDecimal newIncrement = pendingBid.getAmount()
                .multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);

        state.setCurrentPrice(pendingBid.getAmount());
        state.setMinBidIncrement(newIncrement);
        state.setBidCount(state.getBidCount() + 1);
        state.setWinnerId(bidder.getUserId());
        state.setWinnerLabel(pendingBid.getBidderLabel());
        auctionCacheAdapter.updateAuctionState(auctionId, state);

        log.info("Bid processed — auction #{}, new price ${}, bidder #{}",
                auctionId, pendingBid.getAmount(), bidder.getUserId());

        return Optional.of(BidNotificationPayload.builder()
                .auctionId(auctionId)
                .currentPrice(pendingBid.getAmount())
                .minNextBid(pendingBid.getAmount().add(newIncrement))
                .bidderLabel(pendingBid.getBidderLabel())
                .endTime(state.getEndTime())
                .extended(extended)
                .bidCount(state.getBidCount())
                .build());
    }
    private void refundPreviousHighestBidder(Long auctionId, Long previousWinnerId) {
        if (previousWinnerId == null) return;

        reservedFundsRepository
                .findByUserUserIdAndAuctionId(previousWinnerId, auctionId)
                .ifPresent(funds -> {
                    if (funds.getStatus() == ReservedFundStatus.HELD) {
                        funds.setStatus(ReservedFundStatus.REFUNDED);
                        reservedFundsRepository.save(funds);
                        log.info("Refunded HELD funds for user #{} on auction #{}",
                                previousWinnerId, auctionId);
                    }
                });
    }

    private void reserveFunds(User bidder, Auction auction, BigDecimal amount) {
        Optional<ReservedFunds> existing = reservedFundsRepository
                .findByUserUserIdAndAuctionId(bidder.getUserId(), auction.getId());

        if (existing.isPresent()) {
            ReservedFunds funds = existing.get();
            funds.setAmount(amount);
            funds.setStatus(ReservedFundStatus.HELD);
            reservedFundsRepository.save(funds);
        } else {
            ReservedFunds funds = new ReservedFunds();
            funds.setUser(bidder);
            funds.setAuction(auction);
            funds.setAmount(amount);
            funds.setStatus(ReservedFundStatus.HELD);
            reservedFundsRepository.save(funds);
        }
    }

    public List<BidResponse> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByPlacedAtDesc(auctionId)
                .stream()
                .map(BidResponse::from)
                .toList();
    }

    public List<Long> getAuctionsBiddenByUser(Long userId) {
        return bidRepository.findDistinctAuctionIdsByBidderUserId(userId);
    }
}
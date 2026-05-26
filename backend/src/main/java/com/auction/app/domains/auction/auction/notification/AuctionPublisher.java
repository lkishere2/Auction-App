package com.auction.app.domains.auction.auction.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.auction.app.domains.auction.auction.model.Auction;
import com.auction.app.domains.auction.bids.dtos.BidResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.auction.app.domains.auction.bids.dtos.BidNotificationPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String NOTIFY_PREFIX = "auction:notify:";
    private static final String BIDS_SUFFIX = ":bids";

    public void publish(BidNotificationPayload payload) {
        String channel = NOTIFY_PREFIX + payload.getAuctionId();
        redisTemplate.convertAndSend(channel, payload);
        log.info("Published bid notification to channel {} — price ${}", channel, payload.getCurrentPrice());
    }

    public void publishHistory(Long auctionId, List<BidResponse> history) {
        String channel = NOTIFY_PREFIX + auctionId +  BIDS_SUFFIX;
        redisTemplate.convertAndSend(channel, history);
        log.info("Published bid history to channel {} - {} bids",  auctionId, history.size());
    }

    public void publishAuctionStarted(Auction auction) {

        // The next valid bid must be at least the current price + the required increment
        BigDecimal minNextBid = auction.getCurrentPrice().add(auction.getMinBidIncrement());

        BidNotificationPayload payload = BidNotificationPayload.builder()
                .auctionId(auction.getId())
                .currentPrice(auction.getCurrentPrice()) // Actual starting price
                .minNextBid(minNextBid)                  // Current price + Min Increment
                .bidderLabel(null)                       // No one has bid yet
                .endTime(auction.getEndTime())           // The actual scheduled end time
                .extended(false)                         // Hasn't been extended yet
                .bidCount(auction.getBidCount())         // Should be 0
                .ended(false)                            // Just started!
                .build();

        redisTemplate.convertAndSend(NOTIFY_PREFIX + auction.getId(), payload);
        log.info("Auction #{} ACTIVE notification published with current price: {}",
                auction.getId(), auction.getCurrentPrice());
    }


    public void publishAuctionEnded(Long auctionId, String winnerLabel, BigDecimal finalPrice, Integer bidCount, Instant endTime) {
        BidNotificationPayload payload = BidNotificationPayload.builder()
                .auctionId(auctionId)
                .currentPrice(finalPrice)
                .minNextBid(BigDecimal.ZERO)
                .bidderLabel(winnerLabel != null ? winnerLabel : "No winner")
                .endTime(endTime)
                .extended(false)
                .bidCount(bidCount)
                .ended(true)
                .build();

        redisTemplate.convertAndSend(NOTIFY_PREFIX + auctionId, payload);
        log.info("Auction #{} ENDED notification published", auctionId);
    }

}
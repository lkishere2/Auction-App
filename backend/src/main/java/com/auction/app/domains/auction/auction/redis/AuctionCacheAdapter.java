package com.auction.app.domains.auction.auction.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.auction.app.domains.auction.bids.dtos.PendingBid;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionCacheAdapter implements AuctionRedisPort {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RESPONSE_PREFIX = "auction:response:";
    private static final String QUEUE_PREFIX = "auction:queue:";
    private static final Duration POST_AUCTION_RETENTION = Duration.ofDays(1);

    public void cacheAuctionResponse(Long auctionId, AuctionResponse response) {
        redisTemplate.opsForValue().set(getResponseKey(auctionId), response, calculateTtl(response));
    }

    public AuctionResponse getAuctionResponse(Long auctionId) {
        return (AuctionResponse) redisTemplate.opsForValue().get(getResponseKey(auctionId));
    }

    public void updateAuctionResponse(Long auctionId, AuctionResponse response) {
        cacheAuctionResponse(auctionId, response);
    }

    public void enqueueBid(Long auctionId, PendingBid bid) {
        redisTemplate.opsForList().rightPush(getQueueKey(auctionId), bid);
    }

    public PendingBid dequeueBid(Long auctionId) {
        return (PendingBid) redisTemplate.opsForList().leftPop(getQueueKey(auctionId));
    }

    public void clearAuctionCache(Long auctionId) {
        redisTemplate.delete(List.of(getResponseKey(auctionId), getQueueKey(auctionId)));
    }

    private String getResponseKey(Long auctionId) {
        return RESPONSE_PREFIX + auctionId;
    }

    private String getQueueKey(Long auctionId) {
        return QUEUE_PREFIX + auctionId;
    }

    private Duration calculateTtl(AuctionResponse response) {
        Instant now = Instant.now();
        if (response.getEndTime().isBefore(now)) {
            return POST_AUCTION_RETENTION;
        }
        return Duration.between(now, response.getEndTime()).plus(POST_AUCTION_RETENTION);
    }
}
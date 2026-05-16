package com.auction.app.domains.auction.auction;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.auction.app.domains.auction.bids.PendingBid;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionCacheAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STATE_PREFIX = "auction:state:";
    private static final String QUEUE_PREFIX = "auction:queue:";

    public void cacheAuctionState(Long auctionId, AuctionState state) {
        redisTemplate.opsForValue().set(
            STATE_PREFIX + auctionId,
            state,
            Duration.ofDays(7)
        );
    }

    public AuctionState getAuctionState(Long auctionId) {
        return (AuctionState) redisTemplate.opsForValue()
                .get(STATE_PREFIX + auctionId);
    }

    public void updateAuctionState(Long auctionId, AuctionState state) {
        Long ttl = redisTemplate.getExpire(STATE_PREFIX + auctionId);
        redisTemplate.opsForValue().set(
            STATE_PREFIX + auctionId,
            state,
            Duration.ofSeconds(ttl != null && ttl > 0 ? ttl : 604800)
        );
    }

    public void clearAuctionState(Long auctionId) {
        redisTemplate.delete(STATE_PREFIX + auctionId);
    }

    public void enqueueBid(Long auctionId, PendingBid bid) {
        redisTemplate.opsForList().rightPush(QUEUE_PREFIX + auctionId, bid);
    }

    public PendingBid dequeueBid(Long auctionId) {
        return (PendingBid) redisTemplate.opsForList()
                .leftPop(QUEUE_PREFIX + auctionId);
    }

    public void clearBidQueue(Long auctionId) {
        redisTemplate.delete(QUEUE_PREFIX + auctionId);
    }

    public void clearAuctionCache(Long auctionId) {
        clearAuctionState(auctionId);
        clearBidQueue(auctionId);
    }
}
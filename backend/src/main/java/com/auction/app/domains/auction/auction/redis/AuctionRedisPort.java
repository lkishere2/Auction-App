package com.auction.app.domains.auction.auction.redis;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.bids.dtos.PendingBid;

public interface AuctionRedisPort {

    void cacheAuctionResponse(Long auctionId, AuctionResponse response);

    AuctionResponse getAuctionResponse(Long auctionId);

    void updateAuctionResponse(Long auctionId, AuctionResponse response);

    void enqueueBid(Long auctionId, PendingBid bid);

    PendingBid dequeueBid(Long auctionId);

    void clearAuctionCache(Long auctionId);
}
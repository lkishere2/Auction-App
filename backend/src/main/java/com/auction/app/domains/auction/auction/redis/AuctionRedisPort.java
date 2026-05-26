package com.auction.app.domains.auction.auction.redis;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.bids.dtos.PendingBid;

import java.util.List;
import java.util.Map;

public interface AuctionRedisPort {

    void cacheAuctionResponse(Long auctionId, AuctionResponse response);

    AuctionResponse getAuctionResponse(Long auctionId);

    void updateAuctionResponse(Long auctionId, AuctionResponse response);

    void cacheAuctionResponses(Map<Long, AuctionResponse> responses);

    List<AuctionResponse> getAuctionResponses(List<Long> ids);

    void enqueueBid(Long auctionId, PendingBid bid);

    PendingBid dequeueBid(Long auctionId);

    void clearAuctionCache(Long auctionId);
}
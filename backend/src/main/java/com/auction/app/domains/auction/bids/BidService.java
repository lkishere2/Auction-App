package com.auction.app.domains.auction.bids;

import com.auction.app.domains.auction.bids.dtos.BidRequest;
import com.auction.app.domains.auction.bids.dtos.BidResponse;

import java.util.List;

public interface BidService {
    void placeBid(Long auctionId, BidRequest request);
    List<BidResponse> getBidHistory(Long auctionId);
    List<Long> getAuctionsBiddenByCurrentUser();
}

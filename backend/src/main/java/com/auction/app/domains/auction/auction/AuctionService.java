package com.auction.app.domains.auction.auction;

import java.util.List;
import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionService {
    AuctionResponse createAuction(AuctionRequest request);
    AuctionResponse cancelAuction(Long auctionId);
    AuctionResponse getAuction(Long auctionId);
    List<AuctionResponse> getAuctionsBidOnByCurrentUser(List<Long> auctionIds);
    Page<AuctionResponse> getDiscoverableAuctions(AuctionFindingRequest request, Pageable pageable);
    List<AuctionResponse> getMyAuctions();
    void cacheAuctionResponse(Auction auction);
}
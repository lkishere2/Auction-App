package com.auction.app.domains.auction.bids;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltnc.auction.domain.auction.auc.AuctionResponse;
import com.ltnc.auction.domain.auction.auc.AuctionService;
import com.ltnc.auction.domain.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final AuctionService auctionService;

    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<Void> placeBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long auctionId,
            @RequestBody @Valid BidRequest request) {
        bidService.placeBid(user, auctionId, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<List<BidResponse>> getBidHistory(
            @PathVariable Long auctionId) {
        return ResponseEntity.ok(bidService.getBidHistory(auctionId));
    }

    @GetMapping("/bids/me")
    public ResponseEntity<List<AuctionResponse>> getAuctionsBidOn(
            @AuthenticationPrincipal User user) {
        List<Long> auctionIds = bidService.getAuctionsBiddenByUser(user.getUserId());
        List<AuctionResponse> auctions = auctionIds.stream()
                .map(auctionService::getAuction)
                .toList();
        return ResponseEntity.ok(auctions);
    }
}
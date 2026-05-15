package com.auction.app.domains.auction.auction;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @PostMapping("/create")
    public ResponseEntity<AuctionResponse> createAuction(@RequestBody @Valid AuctionRequest auctionRequest) {
        return ResponseEntity.ok(auctionService.createAuction(auctionRequest));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> cancelAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.cancelAuction(auctionId));
    }
}

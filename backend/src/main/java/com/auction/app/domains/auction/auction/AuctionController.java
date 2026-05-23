package com.auction.app.domains.auction.auction;

import java.util.List;

import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auction")
@Validated
public class AuctionController {
    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(@RequestBody @Valid AuctionRequest request) {
        return ResponseEntity.ok(auctionService.createAuction(request));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> cancelAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.cancelAuction(auctionId));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.getAuction(auctionId));
    }

    @GetMapping("/discover")
    public ResponseEntity<Page<AuctionResponse>> getDiscoverableAuctions(
            @Valid AuctionFindingRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(auctionService.getDiscoverableAuctions(request, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AuctionResponse>> getMyAuctions() {
        return ResponseEntity.ok(auctionService.getMyAuctions());
    }
}
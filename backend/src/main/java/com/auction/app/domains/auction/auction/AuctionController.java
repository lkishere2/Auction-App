package com.auction.app.domains.auction.auction;

import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auction")
@Validated
public class AuctionController {
    private final AuctionService auctionService;

    @GetMapping("/me")
    public ResponseEntity<Page<AuctionResponse>> getMyAuctions(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(pageNo, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return ResponseEntity.ok(auctionService.getMyAuctions(pageable));
    }

    @PostMapping("/create")
    public ResponseEntity<AuctionResponse> createAuction(@RequestBody @Valid AuctionRequest request) {
        return ResponseEntity.ok(auctionService.createAuction(request));
    }

    @DeleteMapping("/cancel/{auctionId}")
    public ResponseEntity<AuctionResponse> cancelAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.cancelAuction(auctionId));
    }

    @GetMapping("/get/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.getAuction(auctionId));
    }

    @GetMapping("/discover")
    public ResponseEntity<Page<AuctionResponse>> getDiscoverableAuctions(
            @Valid @RequestBody AuctionFindingRequest request,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be >= 1") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auctionService.getDiscoverableAuctions(request, pageable));
    }

}
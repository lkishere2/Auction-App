package com.auction.app.domains.auction.auction;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltnc.auction.domain.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid AuctionRequest request) {
        return ResponseEntity.ok(auctionService.createAuction(user.getUserId(), request));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> cancelAuction(
            @AuthenticationPrincipal User user,
            @PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.cancelAuction(user.getUserId(), auctionId));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(
            @PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.getAuction(auctionId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AuctionResponse>> getActiveAuctions() {
        return ResponseEntity.ok(auctionService.getActiveAuctions());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<AuctionResponse>> getUpcomingAuctions() {
        return ResponseEntity.ok(auctionService.getUpcomingAuctions());
    }

    @GetMapping("/my")
    public ResponseEntity<List<AuctionResponse>> getMyAuctions(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(auctionService.getMyAuctions(user.getUserId()));
    }
}
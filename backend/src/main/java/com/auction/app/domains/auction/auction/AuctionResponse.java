package com.auction.app.domains.auction.auction;

import java.math.BigDecimal;
import java.time.Instant;

import com.auction.app.domains.inventory.ItemCategories;

public record AuctionResponse(
    Long id,
    String sellerLabel,
    Long itemId,
    String itemName,
    ItemCategories itemCategory,
    BigDecimal startingPrice,
    BigDecimal currentPrice,
    BigDecimal minBidIncrement,
    Instant startTime,
    Instant endTime,
    AuctionStatus status,
    String winnerLabel,
    Integer bidCount
) {
    // from DB only — UPCOMING/ENDED/CANCELLED
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
            auction.getId(),
            auction.getSeller().getDisplayUsername() + " #" + auction.getSeller().getUserId(),
            auction.getItem().getId(),
            auction.getItem().getName(),
            auction.getItem().getCategory(),
            auction.getStartingPrice(),
            auction.getCurrentPrice(),
            auction.getMinBidIncrement(),
            auction.getStartTime(),
            auction.getEndTime(),
            auction.getStatus(),
            auction.getWinner() != null
                ? auction.getWinner().getDisplayUsername() + " #" + auction.getWinner().getUserId()
                : null,
            auction.getBidCount()
        );
    }

    public static AuctionResponse fromWithState(Auction auction, AuctionState state) {
        return new AuctionResponse(
            auction.getId(),
            auction.getSeller().getDisplayUsername() + " #" + auction.getSeller().getUserId(),
            auction.getItem().getId(),
            auction.getItem().getName(),
            auction.getItem().getCategory(),
            auction.getStartingPrice(),
            state.getCurrentPrice(),
            state.getMinBidIncrement(),
            auction.getStartTime(),
            state.getEndTime(),
            state.getStatus(),
            state.getWinnerLabel(),
            state.getBidCount()
        );
    }
}
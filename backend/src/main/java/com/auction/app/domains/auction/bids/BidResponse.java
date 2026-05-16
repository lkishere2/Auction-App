package com.auction.app.domains.auction.bids;

import java.math.BigDecimal;
import java.time.Instant;

public record BidResponse(
    Long bidId,
    Long auctionId,
    String bidderLabel,
    BigDecimal amount,
    Instant placedAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
            bid.getId(),
            bid.getAuction().getId(),
            bid.getBidder().getDisplayUsername() + " #" + bid.getBidder().getUserId(),
            bid.getAmount(),
            bid.getPlacedAt()
        );
    }
}
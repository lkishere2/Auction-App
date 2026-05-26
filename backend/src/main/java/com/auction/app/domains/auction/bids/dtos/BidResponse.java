package com.auction.app.domains.auction.bids.dtos;

import java.math.BigDecimal;
import java.time.Instant;

import com.auction.app.domains.auction.bids.model.Bid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {

    private Long bidId;
    private Long auctionId;
    private String bidderLabel;
    private BigDecimal amount;
    private Instant placedAt;

    public static BidResponse from(Bid bid) {
        return BidResponse.builder()
                .bidId(bid.getId())
                .auctionId(bid.getAuction().getId())
                .bidderLabel(bid.getBidder().getDisplayName() + " #" + bid.getBidder().getId())
                .amount(bid.getAmount())
                .placedAt(bid.getPlacedAt())
                .build();
    }
}
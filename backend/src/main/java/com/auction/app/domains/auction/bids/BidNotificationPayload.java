package com.auction.app.domains.auction.bids;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidNotificationPayload {
    private Long auctionId;
    private BigDecimal currentPrice;
    private BigDecimal minNextBid;
    private String bidderLabel;
    private Instant endTime;
    private boolean extended;
    private Integer bidCount;
    private boolean ended; // ← add this flag to indicate auction end
}
package com.auction.app.domains.auction.bids.dtos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Jacksonized
public class BidNotificationPayload implements Serializable {
    private Long auctionId;
    private BigDecimal currentPrice;
    private BigDecimal minNextBid;
    private String bidderLabel;
    private Instant endTime;
    private boolean extended;
    private Integer bidCount;
    private boolean ended;
}
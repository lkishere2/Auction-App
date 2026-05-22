package com.auction.app.domains.auction.auction.dtos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.auction.app.domains.auction.auction.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionState implements Serializable {
    private Long auctionId;
    private Long sellerId;
    private BigDecimal currentPrice;
    private BigDecimal minBidIncrement;
    private Instant endTime;
    private boolean extended;
    private Integer bidCount;
    private Long winnerId;
    private String winnerLabel;
    private AuctionStatus status;
}
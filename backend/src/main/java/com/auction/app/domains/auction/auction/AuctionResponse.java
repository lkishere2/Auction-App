package com.auction.app.domains.auction.auction;

import com.auction.app.domains.products.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionResponse {
    private Long id;
    private String sellerLabel;
    private Long productId;
    private String productName;
    private Set<Tag> productTags;
    private int quantity;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal minBidIncrement;
    private Instant startTime;
    private Instant endTime;
    private AuctionStatus status;
    private String winnerLabel;
    private Integer bidCount;
}
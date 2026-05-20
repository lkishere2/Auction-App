package com.auction.app.domains.auction.auction.dtos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.auction.app.domains.auction.auction.Auction;
import com.auction.app.domains.auction.auction.AuctionStatus;
import com.auction.app.domains.products.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionResponse implements Serializable {
    private Long id;
    private String sellerLabel;
    private Long productId;
    private String productName;
    private Set<Tag> productTags;
    private String productDescription;
    private String productImageUrl;
    private Integer auctionedQuantity;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal minBidIncrement;
    private Instant startTime;
    private Instant endTime;
    private boolean extended;
    private AuctionStatus status;
    private String winnerLabel;
    private Integer bidCount;

    public static AuctionResponse from(Auction auction) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .sellerLabel(auction.getSeller().getDisplayName() + " #" + auction.getSeller().getId())
                .productId(auction.getProduct().getId())
                .productName(auction.getProduct().getProductName())
                .productTags(auction.getProduct().getTags())
                .productDescription(auction.getProduct().getDescription())
                .productImageUrl(auction.getProduct().getProductImageUrl())
                .auctionedQuantity(auction.getAuctionedQuantity())
                .startingPrice(auction.getStartingPrice())
                .currentPrice(auction.getCurrentPrice())
                .minBidIncrement(auction.getMinBidIncrement())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .winnerLabel(auction.getWinner() != null
                        ? auction.getWinner().getDisplayName() + " #" + auction.getWinner().getId()
                        : null)
                .bidCount(auction.getBidCount())
                .build();
    }
}
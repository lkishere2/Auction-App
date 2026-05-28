package com.auction.app.domains.auction.auction.dtos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.auction.app.domains.auction.auction.model.Auction;
import com.auction.app.domains.auction.auction.model.AuctionStatus;
import com.auction.app.domains.products.model.Product;
import com.auction.app.domains.products.model.Tag;
import com.auction.app.domains.users.users.model.User;
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
    private Long sellerId;
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
    private Long winnerId;
    private String winnerLabel;
    private Integer bidCount;

    public static AuctionResponse from(Auction auction) {

        User seller = auction.getSeller();
        Product product = auction.getProduct();

        return AuctionResponse.builder()
                .id(auction.getId())
                .sellerId(seller.getId())
                .sellerLabel(seller.getDisplayName())
                .productId(product.getId())
                .productName(product.getProductName())
                .productTags(new HashSet<>(product.getTags()))
                .productDescription(product.getDescription())
                .productImageUrl(product.getProductImageUrl())
                .auctionedQuantity(auction.getAuctionedQuantity())
                .startingPrice(auction.getStartingPrice())
                .currentPrice(auction.getCurrentPrice())
                .minBidIncrement(auction.getMinBidIncrement())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .winnerLabel(auction.getWinner() != null ? auction.getWinner().getDisplayName() : null)
                .bidCount(auction.getBidCount())
                .build();
    }
}
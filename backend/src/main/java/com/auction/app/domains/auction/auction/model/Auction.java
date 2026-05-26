package com.auction.app.domains.auction.auction.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.auction.app.domains.products.model.Product;
import com.auction.app.domains.users.users.model.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "auctions",
        indexes = {
                @Index(name = "idx_auctions_seller_start_time", columnList = "seller_id, start_time DESC"),
                @Index(name = "idx_auctions_status", columnList = "status"),
                @Index(name = "idx_auctions_end_time", columnList = "end_time")
        }
)
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer auctionedQuantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal startingPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minBidIncrement;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.UPCOMING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(nullable = false)
    @Builder.Default
    private Integer bidCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean extended = false;

    public void recalculateMinBidIncrement() {
        this.minBidIncrement = this.currentPrice
                .multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
package com.auction.app.domains.auction.auction.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import com.auction.app.domains.auction.auction.AuctionStatus;
import com.auction.app.domains.products.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionFindingRequest {

    @Size(max = 100, message = "Product name search query must not exceed 100 characters")
    private String productName;

    private Set<Tag> tags;

    private Instant startTime;

    private Instant endTime;

    @DecimalMin(value = "0.0", message = "Minimum starting price cannot be negative")
    private BigDecimal minStartingPrice;

    private AuctionStatus status;
}
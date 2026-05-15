package com.auction.app.domains.auction.auction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AuctionRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity cannot be negative")
    private int quantity;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    BigDecimal startingPrice;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    Instant startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    Instant endTime;

}

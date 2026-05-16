package com.auction.app.domains.auction.auction;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record AuctionRequest(
    @NotNull(message = "Item is required") 
    Long itemId,

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    BigDecimal startingPrice,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    Instant startTime,

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    Instant endTime
) {}
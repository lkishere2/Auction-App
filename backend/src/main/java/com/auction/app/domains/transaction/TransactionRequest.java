package com.auction.app.domains.transaction;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @Positive
    private BigDecimal amount;
    private TransactionType type;
}
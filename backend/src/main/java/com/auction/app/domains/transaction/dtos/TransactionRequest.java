package com.auction.app.domains.transaction.dtos;

import com.auction.app.domains.transaction.model.TransactionType;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @Positive
    private BigDecimal amount;
    private TransactionType type;
}
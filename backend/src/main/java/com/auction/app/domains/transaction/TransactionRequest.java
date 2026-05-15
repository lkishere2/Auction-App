package com.auction.app.domains.transaction;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private BigDecimal amount;
    private TransactionType type;
}
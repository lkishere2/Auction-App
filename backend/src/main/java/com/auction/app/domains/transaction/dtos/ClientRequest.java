package com.auction.app.domains.transaction.dtos;

import com.auction.app.domains.transaction.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {
    private Long transactionId;
    private Long userId;
    private String username;
    private String email;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime createdAt;
}

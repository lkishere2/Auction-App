package com.auction.app.domains.transaction;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface TransactionService {
    // USER
    Page<TransactionResponse> getUserTransaction(int page, int size);
    TransactionResponse createTransaction(TransactionRequest transactionRequest);
    void deleteTransaction(Long id);

    // ADMIN
    Page<ClientRequest> getAllTransactionRequest(int page, int size);
    void deposit(Long userId, BigDecimal amount);
    void withdraw(Long userId, BigDecimal amount);
    void cancelTransaction(Long id);
}

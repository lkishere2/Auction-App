package com.auction.app.domains.transaction;

import com.auction.app.domains.transaction.dtos.ClientRequest;
import com.auction.app.domains.transaction.dtos.TransactionRequest;
import com.auction.app.domains.transaction.dtos.TransactionResponse;
import org.springframework.data.domain.Page;

public interface TransactionService {
    // USER
    Page<TransactionResponse> getUserTransaction(int page, int size);
    TransactionResponse createTransaction(TransactionRequest transactionRequest);
    void deleteTransaction(Long id);

    // ADMIN
    Page<ClientRequest> getAllTransactionRequest(int page, int size);
    void acceptTransaction(ClientRequest request);
    void cancelTransaction(Long id);

}

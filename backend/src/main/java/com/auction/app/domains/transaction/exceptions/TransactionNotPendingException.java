package com.auction.app.domains.transaction.exceptions;

public class TransactionNotPendingException extends RuntimeException {
    public TransactionNotPendingException(String message) {
        super(message);
    }
}

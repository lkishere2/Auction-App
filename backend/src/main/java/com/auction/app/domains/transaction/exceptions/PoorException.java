package com.auction.app.domains.transaction.exceptions;

public class PoorException extends RuntimeException {
    public PoorException(String message) {
        super(message);
    }
}

package com.auction.app.domains.transaction.exceptions;

public class AuthorizedException extends RuntimeException {
    public AuthorizedException(String message) {
        super(message);
    }
}

package com.auction.app.domains.auth.exceptions;

public class RefreshTokenSuspiciousActivityException extends RuntimeException {
    public RefreshTokenSuspiciousActivityException(String message) {
        super(message);
    }
}

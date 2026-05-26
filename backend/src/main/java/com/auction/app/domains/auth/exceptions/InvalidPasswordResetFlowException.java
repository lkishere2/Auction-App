package com.auction.app.domains.auth.exceptions;

public class InvalidPasswordResetFlowException extends RuntimeException {
    public InvalidPasswordResetFlowException(String message) {
        super(message);
    }
}

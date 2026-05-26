package com.auction.app.domains.auth.exceptions;

public class EmailSendFailureException extends RuntimeException {
    public EmailSendFailureException(String message) {
        super(message);
    }
}

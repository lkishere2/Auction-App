package com.auction.app.domains.auction.exceptions;

public class InvalidEndTimeException extends RuntimeException {
    public InvalidEndTimeException(String message) {
        super(message);
    }
}

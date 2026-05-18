package com.auction.app.domains.auction.auction.exception;

public class InvalidEndTimeException extends RuntimeException {
    public InvalidEndTimeException(String message) {
        super(message);
    }
}

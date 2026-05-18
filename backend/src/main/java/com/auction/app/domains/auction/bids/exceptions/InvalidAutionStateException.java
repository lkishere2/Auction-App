package com.auction.app.domains.auction.bids.exceptions;

public class InvalidAutionStateException extends RuntimeException {
    public InvalidAutionStateException(String message) {
        super(message);
    }
}

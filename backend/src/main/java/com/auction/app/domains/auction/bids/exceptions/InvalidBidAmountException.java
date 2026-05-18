package com.auction.app.domains.auction.bids.exceptions;

public class InvalidBidAmountException extends RuntimeException {
    public InvalidBidAmountException(String message) {
        super(message);
    }
}

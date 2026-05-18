package com.auction.app.domains.auction.bids.exceptions;

public class BidderNotFoundException extends RuntimeException {
    public BidderNotFoundException(String message) {
        super(message);
    }
}

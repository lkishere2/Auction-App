package com.auction.app.domains.auction.bids.exceptions;

public class SelfBiddingException extends RuntimeException {
    public SelfBiddingException(String message) {
        super(message);
    }
}

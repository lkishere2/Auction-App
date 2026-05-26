package com.auction.app.domains.auction.exceptions;

public class NotUpcommingAuctionException extends RuntimeException {
    public NotUpcommingAuctionException(String message) {
        super(message);
    }
}

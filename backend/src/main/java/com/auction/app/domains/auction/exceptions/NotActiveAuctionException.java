package com.auction.app.domains.auction.exceptions;

public class NotActiveAuctionException extends RuntimeException {
    public NotActiveAuctionException(String message) {
        super(message);
    }
}

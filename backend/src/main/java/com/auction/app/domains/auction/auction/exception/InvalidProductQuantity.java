package com.auction.app.domains.auction.auction.exception;

public class InvalidProductQuantity extends RuntimeException {
    public InvalidProductQuantity(String message) {
        super(message);
    }
}

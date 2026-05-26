package com.auction.app.domains.auction.exceptions;

public class InvalidProductQuantity extends RuntimeException {
    public InvalidProductQuantity(String message) {
        super(message);
    }
}

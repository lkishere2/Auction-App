package com.auction.app.domains.auction.exceptions;

public class ListedProductException extends RuntimeException {
    public ListedProductException(String message) {
        super(message);
    }
}

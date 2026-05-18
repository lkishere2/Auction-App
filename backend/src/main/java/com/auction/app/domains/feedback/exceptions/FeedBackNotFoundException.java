package com.auction.app.domains.feedback.exceptions;

public class FeedBackNotFoundException extends RuntimeException {
    public FeedBackNotFoundException(String message) {
        super(message);
    }
}

package com.auction.app.domains.notifications;

public enum NotificationType {
    FOLLOWING,
    NEW_AUCTION;

    public String generateMessage(String username) {
        return switch (this) {
            case FOLLOWING -> username + " has followed you!";
            case NEW_AUCTION -> username + " has created a new auction, stay tuned!";
        };
    }
}
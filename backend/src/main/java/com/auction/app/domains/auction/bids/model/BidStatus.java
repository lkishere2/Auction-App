package com.auction.app.domains.auction.bids.model;

public enum BidStatus {
    PENDING,   // In the Redis queue
    HELD,      // Current highest bidder (funds locked)
    REFUNDED,  // Outbid (funds released)
    WON        // Auction ended, funds are permanently captured/transferred
}
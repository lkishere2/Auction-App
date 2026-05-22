package com.auction.app.domains.users.connection;

public interface ConnectionService {
    String toggleFollow(Long followingId);
    UserStats getUserStats(Long userId);
}

package com.auction.app.domains.users.connection;

import com.auction.app.domains.users.connection.dtos.UserStats;

public interface ConnectionService {
    String toggleFollow(Long followingId);
    UserStats getUserStats(Long userId);
}

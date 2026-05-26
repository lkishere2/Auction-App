package com.auction.app.domains.users.connection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStats {
    private Long followersCount;
    private Long followingCount;
}

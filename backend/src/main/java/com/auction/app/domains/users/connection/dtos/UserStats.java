package com.auction.app.domains.users.connection.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStats {
    private Long followersCount;
    private Long followingCount;
}

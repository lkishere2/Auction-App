package com.auction.app.domains.users.users.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserResponse {
    private String username;
    private String email;
    private BigDecimal balance;
}

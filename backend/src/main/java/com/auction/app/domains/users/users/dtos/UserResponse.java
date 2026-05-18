package com.auction.app.domains.users.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.ConnectionBuilder;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserResponse {
    private String username;
    private String email;
    private BigDecimal balance;

}

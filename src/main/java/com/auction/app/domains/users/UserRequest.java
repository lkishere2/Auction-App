package com.auction.app.domains.users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class UserRequest {
    private long id;
    private String username;
    private String email;
}

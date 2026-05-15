package com.auction.app.domains.auth.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotNull(message = "Username is required")
    @NotEmpty(message = "Username cannot be empty")
    private String username;

    @NotNull(message = "Email is required")
    @NotEmpty(message = "Email cannot be empty")
    private String email;

    @NotNull(message = "Password is required")
    @NotEmpty(message = "Password cannot be empty")
    private String password;

}

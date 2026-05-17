package com.auction.app.domains.auth.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Email
    private String email;

    @NotNull(message = "Password is required")
    @NotEmpty(message = "Password cannot be empty")
    @Size(min = 6, message = "The password is too short, minimum 6 characters")
    @Size(max = 15, message = "The password is too long, maximum 15 characters")
    private String password;

}

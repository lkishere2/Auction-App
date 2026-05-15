package com.auction.app.domains.auth.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequest {

    @NotNull(message = "Email is required")
    @NotEmpty(message = "Email cannot be empty")
    @Email
    private String email;

}

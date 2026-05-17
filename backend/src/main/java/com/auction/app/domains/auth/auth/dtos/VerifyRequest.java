package com.auction.app.domains.auth.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

@Getter
@Setter
public class VerifyRequest {

    @NotNull(message = "Email is required")
    @NotEmpty(message = "Email cannot be empty")
    @Email
    private String email;

    @Range(min = 100000, max = 999999, message = "Code must be between 100000 and 999999")
    private String verificationCode;
}

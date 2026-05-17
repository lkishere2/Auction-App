package com.auction.app.domains.auth.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    @Size(min = 10, max = 5000)
    private String refreshToken;

}

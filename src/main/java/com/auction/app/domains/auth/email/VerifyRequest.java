package com.auction.app.domains.auth.email;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequest {
    private String email;
    private String verificationCode;
}

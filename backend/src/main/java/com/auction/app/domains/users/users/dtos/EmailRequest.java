package com.auction.app.domains.users.users.dtos;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequest {

    @Email(message = "Invalid email format")
    private String email;

}

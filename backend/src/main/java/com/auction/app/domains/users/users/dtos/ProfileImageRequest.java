package com.auction.app.domains.users.users.dtos;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class ProfileImageRequest {

    @NotBlank(message = "Profile image URL cannot be blank")
    @URL(message = "Must be a valid URL string")
    private String profileImageUrl;
}
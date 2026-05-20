package com.auction.app.domains.products;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class ProductRequest {

    @Size(max = 250, message = "Product name must shorter than 250 characters")
    private String productName;

    @Pattern(
            regexp = "^(?:\\s*\\S+\\s+){0,249}\\S*\\s*$",
            message = "Description must not exceed 250 words"
    )
    private String description;

    @Positive (message = "quantity must be positive")
    private int quantity;
    private Set<Tag> tags;
}

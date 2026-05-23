package com.auction.app.domains.products.dtos;

import com.auction.app.domains.products.Tag;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

import java.util.Set;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name cannot be empty")
    @Pattern(regexp = "^(?:\\S+\\s*){1,50}$", message = "Product name must not exceed 50 words")
    private String productName;

    @Pattern(regexp = "^(?:\\S+\\s*){0,350}$", message = "Description must not exceed 350 words")
    private String description;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantity;

    @Size(max = 1024, message = "Image URL is too long (max 1024 characters)")
    @URL(message = "Must be a valid URL")
    private String productImageUrl;

    private Set<Tag> tags;
}
package com.auction.app.domains.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(
    @NotBlank(message = "Name is required") String name,
    String description,
    @NotNull(message = "Need to categorize the item") ItemCategories category
) {
}

package com.auction.app.domains.inventory;
import java.time.Instant;

public record InventoryItemResponse(
    Long id,
    String ownerUsername,
    String name,
    String description,
    ItemCategories category,
    ItemStatus status,
    Instant createdAt
) {
    public static InventoryItemResponse from(Item item) {
        return new InventoryItemResponse(
            item.getId(),
            item.getOwner().getDisplayUsername() + " #" + item.getOwner().getUserId(),
            item.getName(),
            item.getDescription(),
            item.getCategory(),
            item.getStatus(),
            item.getCreatedAt()
        );
    }
}
package com.auction.app.domains.products;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class ProductResponse {
    private Long id;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private Set<Tag> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.auction.app.domains.products;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String productName;
    private String description;
    private int quantity;
    private String productImageUrl;
    private Set<Tag> tags;
    private LocalDateTime createdAt;
}
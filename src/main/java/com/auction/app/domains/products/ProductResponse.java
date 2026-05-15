package com.auction.app.domains.products;

import com.auction.app.domains.tag.Tag;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter

public class ProductResponse {
    private Long id;
    private String productName;
    private Long price;
    private int quantity;

    private Set<Tag> tags = new HashSet<>();

}

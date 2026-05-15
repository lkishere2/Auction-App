package com.auction.app.domains.products;

import com.auction.app.domains.tag.Tag;
import com.auction.app.domains.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "products")

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "productName", nullable = false)
    private String productName;

    @Column(name = "productLowestPrice", nullable = false)
    private Long price;

    @Column(name = "productQuantity", nullable = false)
    private int quantity;

    //add tag to product
    @ElementCollection(targetClass = Tag.class)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_name")
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
}

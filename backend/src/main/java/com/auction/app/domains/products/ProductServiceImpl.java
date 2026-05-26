package com.auction.app.domains.products;

import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import com.auction.app.domains.products.model.Product;
import com.auction.app.domains.products.model.Tag;
import com.auction.app.domains.users.users.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getStorage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findAllUserProducts(currentUser().getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest productRequest) {
        Product product = mapToEntity(productRequest, currentUser());
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse editProduct(Long id, ProductRequest productRequest) {
        Product product = findProductAndValidateUser(id);
        updateEntity(product, productRequest);
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductAndValidateUser(id);
        productRepository.delete(product);
    }

    // Helpers
    private Product findProductAndValidateUser(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found."));
        if (!product.getOwner().getId().equals(currentUser().getId())) {
            throw new AccessDeniedException("Unauthorized: You do not own this product.");
        }
        return product;
    }

    private void updateEntity(Product product, ProductRequest productRequest) {
        product.setProductName(productRequest.getProductName());
        product.setDescription(productRequest.getDescription());
        product.setQuantity(productRequest.getQuantity());
        product.setProductImageUrl(productRequest.getProductImageUrl());
        product.setTags(resolveTags(productRequest.getTags()));
    }

    private Product mapToEntity(ProductRequest productRequest, User currentUser) {
        return Product.builder()
                .productName(productRequest.getProductName())
                .description(productRequest.getDescription())
                .quantity(productRequest.getQuantity())
                .productImageUrl(productRequest.getProductImageUrl())
                .tags(resolveTags(productRequest.getTags()))
                .owner(currentUser)
                .build();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .quantity(product.getQuantity())
                .productImageUrl(product.getProductImageUrl())
                .tags(product.getTags())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private Set<Tag> resolveTags(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of(Tag.OTHER);
        }
        return tags;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new BadCredentialsException("User session is invalid or expired.");
        }
        return (User) authentication.getPrincipal();
    }
}

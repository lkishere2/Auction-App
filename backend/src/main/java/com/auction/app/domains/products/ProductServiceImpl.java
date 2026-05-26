package com.auction.app.domains.products;

import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import com.auction.app.domains.products.model.Product;
import com.auction.app.domains.products.model.Tag;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getStorage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        try {
            return productRepository.findAllUserProducts(securityUtils.getCurrentUserId(), pageable)
                    .map(this::mapToResponse);
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }
    }

    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest productRequest) {
        User user;
        try {
            user = securityUtils.getCurrentUser();
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }
        Product product = mapToEntity(productRequest, user);
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
        try {
            if (!product.getOwner().getId().equals(securityUtils.getCurrentUserId())) {
                throw new AccessDeniedException("Unauthorized: You do not own this product.");
            }
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
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
}
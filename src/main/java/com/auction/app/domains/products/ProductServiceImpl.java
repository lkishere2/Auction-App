package com.auction.app.domains.products;

import com.auction.app.domains.users.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getStorage(int page, int size, String keyword, Set<Tag> tags) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Product> products = productRepository.findByKeywordAndTags(keyword, tags, pageable);

        User currentUser = getCurrentUser();

        return products.map(product -> {
            if (!product.getOwner().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized: You can only access your own storage.");
            }
            return mapToResponse(product);
        });
    }

    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest productRequest) {
        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setPrice(productRequest.getPrice());
        product.setQuantity(productRequest.getQuantity());
        product.setTags(productRequest.getTags());
        product.setOwner(getCurrentUser());
        product.setCreatedAt(LocalDateTime.now());

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse editProduct(Long id, ProductRequest productRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getOwner().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized: You do not own this product.");
        }

        product.setProductName(productRequest.getProductName());
        product.setPrice(productRequest.getPrice());
        product.setQuantity(productRequest.getQuantity());
        product.setTags(productRequest.getTags());
        product.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getOwner().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized: You cannot delete this product.");
        }

        productRepository.delete(product);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setProductName(product.getProductName());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setTags(product.getTags());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
}
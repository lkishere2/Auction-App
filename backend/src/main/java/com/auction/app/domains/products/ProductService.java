package com.auction.app.domains.products;

import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface ProductService {
    Page<ProductResponse> getStorage(int page, int size, String keyword, Set<Tag> tags);
    ProductResponse addProduct(ProductRequest productRequest);
    ProductResponse editProduct(Long id, ProductRequest productRequest);
    void deleteProduct(Long id);
}

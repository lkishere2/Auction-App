package com.auction.app.domains.products;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("api/v1/inventory")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Product")
@Validated
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/get")
    public ResponseEntity<Page<ProductResponse>> getStorage(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be >= 1") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Set<Tag> tags) {
        Page<ProductResponse> response = productService.getStorage(page, size, keyword, tags);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<ProductResponse> addProduct(@RequestBody @Valid ProductRequest productRequest) {
        ProductResponse response = productService.addProduct(productRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ProductResponse> editProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest productRequest) {
        ProductResponse response = productService.editProduct(id, productRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
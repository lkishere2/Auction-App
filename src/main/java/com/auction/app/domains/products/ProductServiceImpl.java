package com.auction.app.domains.products;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;
    //Crud

    //Create
    @Override
    public ProductResponse createProduct(ProductRequest productRequest){
        Product newProduct = new Product();

        newProduct.setProductName(productRequest.getProductName());
        newProduct.setPrice(productRequest.getPrice());
        newProduct.setQuantity(productRequest.getQuantity());
        newProduct.setTags(productRequest.getTags());

        productRepository.save(newProduct);

        ProductResponse newProductResponse = new ProductResponse();

        newProductResponse.setProductName(newProduct.getProductName());
        newProductResponse.setPrice(newProduct.getPrice());
        newProductResponse.setQuantity(newProduct.getQuantity());
        newProductResponse.setTags(newProduct.getTags());

        return newProductResponse;
    }

    //Update
    @Override
    public ProductResponse updateProduct(ProductRequest productRequest, Long id){
        Product newProduct =productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        newProduct.setProductName(productRequest.getProductName());
        newProduct.setPrice(productRequest.getPrice());
        newProduct.setQuantity(productRequest.getQuantity());
        newProduct.setTags(productRequest.getTags());

        productRepository.save(newProduct);
        ProductResponse newProductResponse = new ProductResponse();

        newProductResponse.setProductName(newProduct.getProductName());
        newProductResponse.setPrice(newProduct.getPrice());
        newProductResponse.setQuantity(newProduct.getQuantity());
        newProductResponse.setTags(newProduct.getTags());

        return newProductResponse;
    }

    //Delete
    @Override
    public String deleteProduct(Long id){
        productRepository.deleteById(id);
        return "Deleted product";
    }

    //Read
    @Override
    public ProductResponse readProduct(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductResponse productResponse = new ProductResponse();

        productResponse.setProductName(product.getProductName());
        productResponse.setPrice(product.getPrice());
        productResponse.setQuantity(product.getQuantity());
        productResponse.setTags(product.getTags());

        return productResponse;
    }
}
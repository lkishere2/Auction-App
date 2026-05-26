package com.auction.app.products;

import com.auction.app.domains.products.ProductController;
import com.auction.app.domains.products.ProductService;
import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    // --- GET /api/v1/inventory/get TESTS ---

    @Test
    void getStorage_WhenCalledWithValidParams_ShouldReturnProductPage() throws Exception {
        // Arrange
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(1L);
        productResponse.setProductName("Gaming Laptop");
        productResponse.setQuantity(2);

        Page<ProductResponse> mockPage = new PageImpl<>(List.of(productResponse));

        when(productService.getStorage(0, 10)).thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Gaming Laptop"))
                .andExpect(jsonPath("$.content[0].quantity").value(2));

        verify(productService).getStorage(0, 10);
    }

    @Test
    void getStorage_WhenOptionalParamsAreMissing_ShouldUseControllerDefaults() throws Exception {
        // Arrange
        Page<ProductResponse> mockPage = new PageImpl<>(List.of());
        when(productService.getStorage(0, 10)).thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(productService).getStorage(0, 10);
    }

    @Test
    void getStorage_WhenPageIsNegative_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void getStorage_WhenSizeIsZero_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    // --- POST /api/v1/inventory/add TESTS ---

    @Test
    void addProduct_WhenRequestIsValid_ShouldReturnCreatedProduct() throws Exception {
        // Arrange
        ProductRequest request = createProductRequest("Mechanical Keyboard", 3);

        ProductResponse response = new ProductResponse();
        response.setId(10L);
        response.setProductName(request.getProductName());
        response.setQuantity(request.getQuantity());

        when(productService.addProduct(any(ProductRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productName").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.quantity").value(3));

        verify(productService).addProduct(any(ProductRequest.class));
    }

    // --- PUT /api/v1/inventory/update/{id} TESTS ---

    @Test
    void editProduct_WhenRequestIsValid_ShouldReturnUpdatedProduct() throws Exception {
        // Arrange
        ProductRequest request = createProductRequest("Mechanical Keyboard", 3);

        ProductResponse response = new ProductResponse();
        response.setId(10L);
        response.setProductName("Mechanical Keyboard");
        response.setQuantity(3);

        when(productService.editProduct(eq(10L), any(ProductRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/update/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productName").value("Mechanical Keyboard"));

        verify(productService).editProduct(eq(10L), any(ProductRequest.class));
    }

    // --- DELETE /api/v1/inventory/delete/{id} TESTS ---

    @Test
    void deleteProduct_WhenIdExists_ShouldReturnNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/inventory/delete/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(10L);
    }

    // --- Helper Method ---
    private ProductRequest createProductRequest(String name, int quantity) {
        ProductRequest request = new ProductRequest();
        request.setProductName(name);
        request.setDescription("Sample description");
        request.setQuantity(quantity);
        return request;
    }
}
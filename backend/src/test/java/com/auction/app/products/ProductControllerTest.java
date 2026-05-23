package com.auction.app.products;

import com.auction.app.domains.products.ProductController;
import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.domains.products.ProductService;
import com.auction.app.domains.products.Tag;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Test
    void getStorage_WhenCalled_ShouldReturnProductPage() throws Exception {
        Page<ProductResponse> response = new PageImpl<>(List.of(ProductResponse.builder()
                .id(1L)
                .productName("Gaming Laptop")
                .description("High-end laptop")
                .quantity(2)
                .tags(Set.of(Tag.ELECTRONICS, Tag.GAMES))
                .build()));

        when(productService.getStorage(eq(0), eq(10), eq("laptop"), eq(Set.of(Tag.ELECTRONICS))))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "laptop")
                        .param("tags", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Gaming Laptop"))
                .andExpect(jsonPath("$.content[0].quantity").value(2));

        verify(productService).getStorage(0, 10, "laptop", Set.of(Tag.ELECTRONICS));
    }

    @Test
    void getStorage_WhenOptionalParamsAreMissing_ShouldUseControllerDefaults() throws Exception {
        Page<ProductResponse> response = new PageImpl<>(List.of());
        when(productService.getStorage(0, 10, null, null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(productService).getStorage(0, 10, null, null);
    }

    @Test
    void getStorage_WhenMultipleTagsAreProvided_ShouldBindTagsAsSet() throws Exception {
        Page<ProductResponse> response = new PageImpl<>(List.of());
        Set<Tag> tags = Set.of(Tag.ELECTRONICS, Tag.GAMES);
        when(productService.getStorage(1, 5, null, tags)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("page", "1")
                        .param("size", "5")
                        .param("tags", "ELECTRONICS", "GAMES"))
                .andExpect(status().isOk());

        verify(productService).getStorage(1, 5, null, tags);
    }

    @Test
    void getStorage_WhenPageIsNegative_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void getStorage_WhenSizeIsZero_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void getStorage_WhenTagValueIsInvalid_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/get")
                        .param("tags", "NOT_A_TAG"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void addProduct_WhenRequestIsValid_ShouldReturnCreatedProduct() throws Exception {
        ProductRequest request = createProductRequest();
        ProductResponse response = ProductResponse.builder()
                .id(10L)
                .productName(request.getProductName())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .tags(request.getTags())
                .build();

        when(productService.addProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/add")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productName").value("Mechanical Keyboard"));

        verify(productService).addProduct(any(ProductRequest.class));
    }

    @Test
    void addProduct_WhenRequestHasBoundaryQuantityOne_ShouldReturnCreatedProduct() throws Exception {
        ProductRequest request = createProductRequest();
        request.setQuantity(1);
        ProductResponse response = ProductResponse.builder()
                .id(11L)
                .productName(request.getProductName())
                .description(request.getDescription())
                .quantity(1)
                .tags(request.getTags())
                .build();

        when(productService.addProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/add")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(1));

        verify(productService).addProduct(any(ProductRequest.class));
    }

    @Test
    void addProduct_WhenRequestIsInvalid_ShouldReturnBadRequest() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setProductName("");
        request.setDescription("");
        request.setQuantity(0);
        request.setTags(Set.of());

        mockMvc.perform(post("/api/v1/inventory/add")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void editProduct_WhenRequestIsValid_ShouldReturnUpdatedProduct() throws Exception {
        ProductRequest request = createProductRequest();
        ProductResponse response = ProductResponse.builder()
                .id(10L)
                .productName("Mechanical Keyboard")
                .description("Hot-swappable keyboard")
                .quantity(3)
                .tags(request.getTags())
                .build();

        when(productService.editProduct(eq(10L), any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/inventory/update/{id}", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productName").value("Mechanical Keyboard"));

        verify(productService).editProduct(eq(10L), any(ProductRequest.class));
    }

    @Test
    void editProduct_WhenRequestHasNegativeQuantity_ShouldReturnBadRequest() throws Exception {
        ProductRequest request = createProductRequest();
        request.setQuantity(-1);

        mockMvc.perform(put("/api/v1/inventory/update/{id}", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProduct_WhenIdExists_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/delete/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(10L);
    }

    private ProductRequest createProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setProductName("Mechanical Keyboard");
        request.setDescription("Hot-swappable keyboard");
        request.setQuantity(3);
        request.setTags(new HashSet<>(Set.of(Tag.ELECTRONICS, Tag.GAMES)));
        return request;
    }
}

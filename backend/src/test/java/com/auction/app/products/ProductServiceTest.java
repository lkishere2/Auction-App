package com.auction.app.products;

import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.domains.products.ProductServiceImpl;
import com.auction.app.domains.products.Tag;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import com.auction.app.domains.users.users.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("seller")
                .email("seller@example.com")
                .password("password")
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStorage_WhenKeywordHasSpacesAndUppercase_ShouldNormalizeKeywordForRepositoryLikeQuery() {
        Product product = Product.builder()
                .id(1L)
                .productName("Gaming Laptop")
                .description("High-end laptop")
                .quantity(2)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(currentUser)
                .build();

        when(productRepository.findByKeywordAndTags(eq(1L), eq("%laptop%"), eq(Set.of(Tag.ELECTRONICS)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        List<ProductResponse> responses = productService.getStorage(0, 10, "  LAPTOP  ", Set.of(Tag.ELECTRONICS))
                .getContent();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProductName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void getStorage_WhenKeywordIsBlankAndTagsAreEmpty_ShouldPassNullFiltersToRepository() {
        when(productRepository.findByKeywordAndTags(eq(1L), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        productService.getStorage(0, 10, "   ", Collections.emptySet());

        verify(productRepository).findByKeywordAndTags(eq(1L), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getStorage_WhenPageOrSizeIsInvalid_ShouldThrowBeforeRepositoryCall() {
        assertThatThrownBy(() -> productService.getStorage(-1, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productService.getStorage(0, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).findByKeywordAndTags(any(), any(), any(), any());
    }

    @Test
    void addProduct_WhenRequestIsValid_ShouldSaveProductForCurrentUser() {
        ProductRequest request = createProductRequest();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.addProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getOwner()).isEqualTo(currentUser);
        assertThat(savedProduct.getProductName()).isEqualTo("Mechanical Keyboard");
        assertThat(savedProduct.getTags()).containsExactlyInAnyOrder(Tag.ELECTRONICS, Tag.GAMES);
        assertThat(response.getProductName()).isEqualTo("Mechanical Keyboard");
        assertThat(response.getQuantity()).isEqualTo(3);
    }

    @Test
    void editProduct_WhenCurrentUserOwnsProduct_ShouldUpdateAndSaveProduct() {
        Product existingProduct = Product.builder()
                .id(10L)
                .productName("Old name")
                .description("Old description")
                .quantity(1)
                .tags(new HashSet<>(Set.of(Tag.COLLECTIBLES)))
                .owner(currentUser)
                .build();
        ProductRequest request = createProductRequest();

        when(productRepository.findById(10L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);

        ProductResponse response = productService.editProduct(10L, request);

        assertThat(existingProduct.getProductName()).isEqualTo("Mechanical Keyboard");
        assertThat(existingProduct.getDescription()).isEqualTo("Hot-swappable keyboard");
        assertThat(existingProduct.getQuantity()).isEqualTo(3);
        assertThat(existingProduct.getTags()).containsExactlyInAnyOrder(Tag.ELECTRONICS, Tag.GAMES);
        assertThat(response.getProductName()).isEqualTo("Mechanical Keyboard");
        verify(productRepository).save(existingProduct);
    }

    @Test
    void editProduct_WhenProductDoesNotExist_ShouldThrowProductNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.editProduct(99L, createProductRequest()))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found.");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_WhenProductBelongsToAnotherUser_ShouldThrowAccessDeniedException() {
        User otherUser = User.builder()
                .id(2L)
                .username("other")
                .email("other@example.com")
                .password("password")
                .build();
        Product product = Product.builder()
                .id(20L)
                .productName("Camera")
                .description("Film camera")
                .quantity(1)
                .owner(otherUser)
                .build();

        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.deleteProduct(20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Unauthorized: You do not own this product.");

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_WhenCurrentUserOwnsProduct_ShouldDeleteProduct() {
        Product product = Product.builder()
                .id(20L)
                .productName("Camera")
                .description("Film camera")
                .quantity(1)
                .owner(currentUser)
                .build();

        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        productService.deleteProduct(20L);

        verify(productRepository).delete(product);
    }

    @Test
    void editProduct_WhenProductBelongsToAnotherUser_ShouldThrowAccessDeniedException() {
        User otherUser = User.builder()
                .id(2L)
                .username("other")
                .email("other@example.com")
                .password("password")
                .build();
        Product product = Product.builder()
                .id(20L)
                .productName("Camera")
                .description("Film camera")
                .quantity(1)
                .owner(otherUser)
                .build();

        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.editProduct(20L, createProductRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Unauthorized: You do not own this product.");

        verify(productRepository, never()).save(any(Product.class));
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

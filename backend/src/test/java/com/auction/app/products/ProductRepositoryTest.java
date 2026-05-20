package com.auction.app.products;

import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.products.Tag;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User testOwner;
    private User otherOwner;
    private Product product1;
    private Product product2;
    private Product product3;
    private Product otherOwnerProduct;

    @BeforeEach
    void setUp() {
        // Populate the required non-nullable fields (username, email, password)
        testOwner = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("securePassword123")
                .enabled(true)
                .build();

        testOwner = userRepository.saveAndFlush(testOwner);

        otherOwner = User.builder()
                .username("otheruser")
                .email("otheruser@example.com")
                .password("securePassword123")
                .enabled(true)
                .build();

        otherOwner = userRepository.saveAndFlush(otherOwner);

        product1 = Product.builder()
                .productName("iPhone 15 Pro")
                .description("Apple smartphone")
                .quantity(10)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(testOwner)
                .build();

        product2 = Product.builder()
                .productName("Gaming Laptop")
                .description("High-end laptop")
                .quantity(5)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS, Tag.GAMES)))
                .owner(testOwner)
                .build();

        product3 = Product.builder()
                .productName("Vintage Leather Jacket")
                .description("Classic collection piece")
                .quantity(2)
                .tags(new HashSet<>(Set.of(Tag.FASHION, Tag.COLLECTIBLES)))
                .owner(testOwner)
                .build();

        otherOwnerProduct = Product.builder()
                .productName("Other Owner Laptop")
                .description("Should not appear in test owner's storage")
                .quantity(1)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(otherOwner)
                .build();

        productRepository.saveAndFlush(product1);
        productRepository.saveAndFlush(product2);
        productRepository.saveAndFlush(product3);
        productRepository.saveAndFlush(otherOwnerProduct);
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // METHOD 1: findByKeywordAndTags (8 Tests)
    // =========================================================================

    // --- Happy Paths (4 Tests) ---

    @Test
    void findByKeywordAndTags_WhenKeywordAndTagsAreNull_ShouldReturnAllProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), null, null, pageable);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .doesNotContain("Other Owner Laptop");
    }

    @Test
    void findByKeywordAndTags_WhenKeywordFilterApplied_ShouldReturnMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), "%laptop%", null, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getProductName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void findByKeywordAndTags_WhenTagsFilterApplied_ShouldReturnMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<Tag> searchTags = Set.of(Tag.ELECTRONICS);
        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), null, searchTags, pageable);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Product::getProductName)
                .containsExactlyInAnyOrder("iPhone 15 Pro", "Gaming Laptop");
    }

    @Test
    void findByKeywordAndTags_WhenBothKeywordAndTagsFilterApplied_ShouldReturnMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<Tag> searchTags = Set.of(Tag.ELECTRONICS);
        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), "%iphone%", searchTags, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getProductName()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    void findByKeywordAndTags_WhenPageSizeIsTwo_ShouldReturnRequestedPageOnly() {
        Pageable pageable = PageRequest.of(0, 2);

        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    // --- Edge Cases (4 Tests) ---

    @Test
    void findByKeywordAndTags_WhenOwnerIdDoesNotExist_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByKeywordAndTags(999L, null, null, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByKeywordAndTags_WhenOwnerIdIsNull_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByKeywordAndTags(null, null, null, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByKeywordAndTags_WhenKeywordDoesNotExist_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), "%nonexistentkey%", null, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByKeywordAndTags_WhenAllTheTagsDoNotExist_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<Tag> unusedTags = Set.of(Tag.GARDENING);
        Page<Product> result = productRepository.findByKeywordAndTags(testOwner.getId(), null, unusedTags, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================================
    // METHOD 2: findByIdAndOwnerUserId (5 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void findByIdAndOwnerUserId_WhenValidIdAndOwnerId_ShouldReturnProduct() {
        Optional<Product> result = productRepository.findByIdAndOwnerUserId(product1.getId(), testOwner.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getProductName()).isEqualTo("iPhone 15 Pro");
    }

    // --- Edge Cases (4 Tests) ---

    @Test
    void findByIdAndOwnerUserId_WhenIdNotFound_ShouldReturnEmptyOptional() {
        Optional<Product> result = productRepository.findByIdAndOwnerUserId(999L, testOwner.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndOwnerUserId_WhenIdIsNull_ShouldReturnEmptyOptional() {
        Optional<Product> result = productRepository.findByIdAndOwnerUserId(null, testOwner.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndOwnerUserId_WhenCurrentUserIdIsNotFound_ShouldReturnEmptyOptional() {
        Optional<Product> result = productRepository.findByIdAndOwnerUserId(product1.getId(), 999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndOwnerUserId_WhenProductBelongsToAnotherUser_ShouldReturnEmptyOptional() {
        Optional<Product> result = productRepository.findByIdAndOwnerUserId(otherOwnerProduct.getId(), testOwner.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndOwnerUserId_WhenCurrentUserIdIsNull_ShouldReturnEmptyOptional() {
        Optional<Product> result = productRepository.findByIdAndOwnerUserId(product1.getId(), null);
        assertThat(result).isEmpty();
    }
}

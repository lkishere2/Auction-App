package com.auction.app.auction;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.app.domains.auction.auction.Auction;
import com.auction.app.domains.auction.auction.AuctionRepository;
import com.auction.app.domains.auction.auction.AuctionStatus;
import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.products.Tag;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class AuctionRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private ProductRepository productRepository;

    private User whiteMouse;
    private Auction auction;
    private Product product;
    private Product product1;

    @BeforeEach
    void setUp() {
        whiteMouse = User.builder()
                .username("MickeyMouse")
                .email("aaaa@gmail.com")
                .password("123456")
                .enabled(true)
                .build();
        whiteMouse = userRepository.save(whiteMouse);

        product = Product.builder()
                .productName("iPhone 15 Pro")
                .description("Apple smartphone")
                .quantity(10)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(whiteMouse)
                .build();
        product = productRepository.save(product);

        product1 = Product.builder()
                .productName("iPhone 17 Pro")
                .description("Apple stupidphone")
                .quantity(10)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(whiteMouse)
                .build();
        product1 = productRepository.save(product1);

        // Fixed typo: call Auction.builder() statically instead of auction.builder()
        auction = Auction.builder()
                .seller(whiteMouse)
                .product(product)
                .auctionedQuantity(2)
                .startingPrice(new BigDecimal("100000.00"))
                .currentPrice(new BigDecimal("100000.00"))
                .minBidIncrement(new BigDecimal("5000.00"))
                .startTime(Instant.now().plusSeconds(3600))
                .endTime(Instant.now().plusSeconds(86400))
                .status(AuctionStatus.UPCOMING)
                .winner(null)
                .bidCount(0)
                .extended(false)
                .build();
        auction = auctionRepository.save(auction);
    }

    @Test
    void findByIdWithDetails_ShouldReturnAuctionWithSellerAndProduct() {
        Optional<Auction> foundAuction = auctionRepository.findByIdWithDetails(auction.getId());

        assertThat(foundAuction).isPresent();
        assertThat(foundAuction.get().getSeller()).isNotNull();
        assertThat(foundAuction.get().getProduct()).isNotNull();
        assertThat(foundAuction.get().getSeller().getDisplayName()).isEqualTo("MickeyMouse");
        assertThat(foundAuction.get().getProduct().getProductName()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    void findByStatusWithDetails_ShouldReturnMatchingAuctions() {
        List<Auction> upcomingAuctions = auctionRepository.findByStatusWithDetails(AuctionStatus.UPCOMING);
        List<Auction> activeAuctions = auctionRepository.findByStatusWithDetails(AuctionStatus.ACTIVE);

        assertThat(upcomingAuctions).hasSize(1);
        assertThat(upcomingAuctions.get(0).getId()).isEqualTo(auction.getId());
        assertThat(activeAuctions).isEmpty();
    }

    @Test
    void findBySellerIdWithDetails_ShouldReturnAuctionsBySeller() {
        List<Auction> auctions = auctionRepository.findBySellerIdWithDetails(whiteMouse.getId());

        assertThat(auctions).hasSize(1);
        assertThat(auctions.get(0).getSeller().getId()).isEqualTo(whiteMouse.getId());
    }

    @Test
    void findByIdsWithDetails_ShouldReturnAuctionsMatchingIds() {
        List<Auction> auctions = auctionRepository.findByIdsWithDetails(List.of(auction.getId()));

        assertThat(auctions).hasSize(1);
        assertThat(auctions.get(0).getId()).isEqualTo(auction.getId());
    }

    @Test
    void existsByProduct_IdAndStatusIn_ShouldReturnTrueIfMatchExists() {
        boolean exists = auctionRepository.existsByProduct_IdAndStatusIn(
                product.getId(),
                List.of(AuctionStatus.UPCOMING, AuctionStatus.ACTIVE)
        );

        boolean notExists = auctionRepository.existsByProduct_IdAndStatusIn(
                product.getId(),
                List.of(AuctionStatus.ENDED)
        );

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void findUpcomingIdsToActivate_ShouldReturnAuctionIdsReadyToStart() {
        // Create an auction whose start time has already passed but is still UPCOMING
        Auction readyAuction = Auction.builder()
                .seller(whiteMouse)
                .product(product1)
                .auctionedQuantity(1)
                .startingPrice(new BigDecimal("50000.00"))
                .currentPrice(new BigDecimal("50000.00"))
                .minBidIncrement(new BigDecimal("2000.00"))
                .startTime(Instant.now().minusSeconds(60)) // Started 1 minute ago
                .endTime(Instant.now().plusSeconds(3600))
                .status(AuctionStatus.UPCOMING)
                .build();
        readyAuction = auctionRepository.save(readyAuction);

        // Retrieve only IDs matching optimization patterns
        List<Long> processableIds = auctionRepository.findUpcomingIdsToActivate(AuctionStatus.UPCOMING, Instant.now());

        assertThat(processableIds).hasSize(1);
        assertThat(processableIds.get(0)).isEqualTo(readyAuction.getId());
    }

    @Test
    void findActiveIdsToEnd_ShouldReturnAuctionIdsReadyToFinish() {
        // Create an ACTIVE auction whose end time has already passed
        Auction expiredAuction = Auction.builder()
                .seller(whiteMouse)
                .product(product1)
                .auctionedQuantity(1)
                .startingPrice(new BigDecimal("50000.00"))
                .currentPrice(new BigDecimal("50000.00"))
                .minBidIncrement(new BigDecimal("2000.00"))
                .startTime(Instant.now().minusSeconds(7200))
                .endTime(Instant.now().minusSeconds(60)) // Ended 1 minute ago
                .status(AuctionStatus.ACTIVE)
                .build();
        expiredAuction = auctionRepository.save(expiredAuction);

        List<Long> processableIds = auctionRepository.findActiveIdsToEnd(AuctionStatus.ACTIVE, Instant.now());

        assertThat(processableIds).hasSize(1);
        assertThat(processableIds.get(0)).isEqualTo(expiredAuction.getId());
    }

    @Test
    void updateStatusForIds_ShouldBulkUpdateStatusesAndClearPersistenceContext() {
        Auction secondAuction = Auction.builder()
                .seller(whiteMouse)
                .product(product1)
                .auctionedQuantity(1)
                .startingPrice(new BigDecimal("70000.00"))
                .currentPrice(new BigDecimal("70000.00"))
                .minBidIncrement(new BigDecimal("3500.00"))
                .startTime(Instant.now().plusSeconds(3600))
                .endTime(Instant.now().plusSeconds(86400))
                .status(AuctionStatus.UPCOMING)
                .build();
        secondAuction = auctionRepository.save(secondAuction);

        List<Long> idsToUpdate = List.of(auction.getId(), secondAuction.getId());

        int updatedCount = auctionRepository.updateStatusForIds(idsToUpdate, AuctionStatus.ACTIVE);
        assertThat(updatedCount).isEqualTo(2);

        // Verify changes are flushed out to the database context accurately
        Auction verifiedFirst = auctionRepository.findById(auction.getId()).orElseThrow();
        Auction verifiedSecond = auctionRepository.findById(secondAuction.getId()).orElseThrow();

        assertThat(verifiedFirst.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(verifiedSecond.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    void findAuctions_ShouldFilterByParametersCorrectlyUsingNativeQuery() {
        Pageable pageable = PageRequest.of(0, 10);

        // 1. Test filtering by product name substring match (case insensitive)
        Page<Auction> resultsByName = auctionRepository.findAuctions(
                "15 pro", null, false, null, null, null, null, pageable
        );
        assertThat(resultsByName.getContent()).hasSize(1);
        assertThat(resultsByName.getContent().get(0).getId()).isEqualTo(auction.getId());

        // 2. Test filtering by Tags matching requirements
        Page<Auction> resultsWithMatchingTag = auctionRepository.findAuctions(
                null, List.of("ELECTRONICS"), true, null, null, null, null, pageable
        );
        assertThat(resultsWithMatchingTag.getContent()).hasSize(1);

        Page<Auction> resultsWithUnmatchedTag = auctionRepository.findAuctions(
                null, List.of("BOOKS"), true, null, null, null, null, pageable
        );
        assertThat(resultsWithUnmatchedTag.getContent()).isEmpty();

        // 3. Test filtering by dynamic status values
        Page<Auction> resultsByStatus = auctionRepository.findAuctions(
                null, null, false, null, null, null, "UPCOMING", pageable
        );
        assertThat(resultsByStatus.getContent()).hasSize(1);
    }
}
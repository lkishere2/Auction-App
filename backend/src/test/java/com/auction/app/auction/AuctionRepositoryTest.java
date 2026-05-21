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
    private User WhiteMouse;
    private Auction auction;
    private Product product;
    private Product product1;
    @BeforeEach
    void setUp() {
        WhiteMouse = User.builder()
                .username("MickeyMouse")
                .email("aaaa@gmail.com")
                .password("123456")
                .enabled(true)
                .build();
        WhiteMouse = userRepository.save(WhiteMouse);

        product = Product.builder()
                .productName("iPhone 15 Pro")
                .description("Apple smartphone")
                .quantity(10)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(WhiteMouse)
                .build();
        product = productRepository.save(product);

        product1 = Product.builder()
                .productName("iPhone 17 Pro")
                .description("Apple stupidphone")
                .quantity(10)
                .tags(new HashSet<>(Set.of(Tag.ELECTRONICS)))
                .owner(WhiteMouse)
                .build();
        product1 = productRepository.save(product1);

        auction = auction.builder()
                .seller(WhiteMouse)
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
        List<Auction> auctions = auctionRepository.findBySellerIdWithDetails(WhiteMouse.getId());

        assertThat(auctions).hasSize(1);
        assertThat(auctions.get(0).getSeller().getId()).isEqualTo(WhiteMouse.getId());
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
    void findIdsByStatus_ShouldReturnOnlyIdsWithoutFetchJoins() {
        List<Long> ids = auctionRepository.findIdsByStatus(AuctionStatus.UPCOMING);

        assertThat(ids).hasSize(1);
        assertThat(ids.get(0)).isEqualTo(auction.getId());
    }

    @Test
    void findUpcomingToActivate_ShouldReturnAuctionsReadyToStart() {
        // Tạo thêm 1 auction đã quá thời gian bắt đầu nhưng vẫn ở trạng thái UPCOMING
        Auction readyAuction = Auction.builder()
                .seller(WhiteMouse)
                .product(product1)
                .auctionedQuantity(1)
                .startingPrice(new BigDecimal("50000.00"))
                .currentPrice(new BigDecimal("50000.00"))
                .minBidIncrement(new BigDecimal("2000.00"))
                .startTime(Instant.now().minusSeconds(60)) // Đã qua thời gian bắt đầu 1 phút
                .endTime(Instant.now().plusSeconds(3600))
                .status(AuctionStatus.UPCOMING)
                .build();
        auctionRepository.save(readyAuction);

        // Kiểm tra xem method có lọc đúng dòng readyAuction (startTime <= now) và bỏ qua dòng ở setUp() không
        List<Auction> processable = auctionRepository.findUpcomingToActivate(AuctionStatus.UPCOMING, Instant.now());

        assertThat(processable).hasSize(1);
        assertThat(processable.get(0).getId()).isEqualTo(readyAuction.getId());
    }

    @Test
    void findActiveToEnd_ShouldReturnAuctionsReadyToFinish() {
        // Tạo 1 cuộc đấu giá đang ACTIVE và đã quá giờ kết thúc
        Auction expiredAuction = Auction.builder()
                .seller(WhiteMouse)
                .product(product1)
                .auctionedQuantity(1)
                .startingPrice(new BigDecimal("50000.00"))
                .currentPrice(new BigDecimal("50000.00"))
                .minBidIncrement(new BigDecimal("2000.00"))
                .startTime(Instant.now().minusSeconds(7200))
                .endTime(Instant.now().minusSeconds(60)) // Đã kết thúc cách đây 1 phút
                .status(AuctionStatus.ACTIVE)
                .build();
        auctionRepository.save(expiredAuction);

        List<Auction> processable = auctionRepository.findActiveToEnd(AuctionStatus.ACTIVE, Instant.now());

        assertThat(processable).hasSize(1);
        assertThat(processable.get(0).getId()).isEqualTo(expiredAuction.getId());
    }
}


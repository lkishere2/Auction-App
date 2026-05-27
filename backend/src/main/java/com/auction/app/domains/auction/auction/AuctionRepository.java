package com.auction.app.domains.auction.auction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.auction.app.domains.auction.auction.model.Auction;
import com.auction.app.domains.auction.auction.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Query(value = "SELECT DISTINCT a.* FROM auctions a " +
            "LEFT JOIN products p ON a.product_id = p.id " +
            "WHERE (:status IS NULL AND a.status IN ('UPCOMING', 'ACTIVE') OR a.status = :status) " +
            "AND (:productName IS NULL OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
            "AND (:hasTags = false OR EXISTS ( " +
            "    SELECT 1 FROM product_tags pt " +
            "    WHERE pt.product_id = p.id AND pt.tag_name IN (:tags)" +
            ")) " +
            "AND (:minStartingPrice IS NULL OR a.starting_price >= :minStartingPrice) " +
            "AND (:startTime IS NULL OR a.start_time >= :startTime) " +
            "AND (:endTime IS NULL OR a.end_time <= :endTime)",
            countQuery = "SELECT COUNT(DISTINCT a.id) FROM auctions a " +
                    "LEFT JOIN products p ON a.product_id = p.id " +
                    "WHERE (:status IS NULL AND a.status IN ('UPCOMING', 'ACTIVE') OR a.status = :status) " +
                    "AND (:productName IS NULL OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
                    "AND (:hasTags = false OR EXISTS ( " +
                    "    SELECT 1 FROM product_tags pt " +
                    "    WHERE pt.product_id = p.id AND pt.tag_name IN (:tags)" +
                    ")) " +
                    "AND (:minStartingPrice IS NULL OR a.starting_price >= :minStartingPrice) " +
                    "AND (:startTime IS NULL OR a.start_time >= :startTime) " +
                    "AND (:endTime IS NULL OR a.end_time <= :endTime)",
            nativeQuery = true)
    Page<Auction> findAuctions(
            @Param("productName") String productName,
            @Param("tags") java.util.Collection<String> tags,
            @Param("hasTags") boolean hasTags,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("minStartingPrice") BigDecimal minStartingPrice,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.product WHERE a.id = :id")
    Optional<Auction> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.product WHERE a.status = :status")
    List<Auction> findByStatusWithDetails(@Param("status") AuctionStatus status);

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.product WHERE a.seller.id = :sellerId")
    List<Auction> findBySellerIdWithDetails(@Param("sellerId") Long sellerId);

    @Query(
            value = "SELECT a.id FROM Auction a WHERE a.seller.id = :sellerId",
            countQuery = "SELECT COUNT(a.id) FROM Auction a WHERE a.seller.id = :sellerId"
    )
    Page<Long> findIdsBySellerIdOrderByStartTime(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.product WHERE a.id IN :ids")
    List<Auction> findByIdsWithDetails(@Param("ids") List<Long> ids);

    boolean existsByProduct_IdAndStatusIn(Long productId, List<AuctionStatus> statuses);

    @Query("SELECT a.id FROM Auction a WHERE a.status = :status AND a.startTime <= :now")
    List<Long> findUpcomingIdsToActivate(@Param("status") AuctionStatus status, @Param("now") Instant now);

    @Query("SELECT a.id FROM Auction a WHERE a.status = :status AND a.endTime <= :now")
    List<Long> findActiveIdsToEnd(@Param("status") AuctionStatus status, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Auction a SET a.status = :newStatus WHERE a.id IN :ids")
    int updateStatusForIds(@Param("ids") List<Long> ids, @Param("newStatus") AuctionStatus newStatus);

}

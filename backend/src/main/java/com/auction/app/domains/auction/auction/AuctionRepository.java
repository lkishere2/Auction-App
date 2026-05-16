package com.auction.app.domains.auction.auction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    // scheduler: find all UPCOMING auctions ready to go ACTIVE
    List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, Instant now);

    // scheduler: find all ACTIVE auctions past their end time
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, Instant now);

    // fetch single auction with seller + item in one query
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.item WHERE a.id = :id")
    Optional<Auction> findByIdWithDetails(@Param("id") Long id);

    // all auctions by status with seller + item — fixes N+1 on list endpoints
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.item WHERE a.status = :status")
    List<Auction> findByStatusWithDetails(@Param("status") AuctionStatus status);

    // seller's auctions with seller + item
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.item WHERE a.seller.userId = :sellerId")
    List<Auction> findBySellerUserIdWithDetails(@Param("sellerId") Long sellerId);

    boolean existsByItem_IdAndStatusIn(Long itemId, List<AuctionStatus> statuses);

    // scheduler queries also need winner fetch for ENDED processing
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.item WHERE a.status = :status AND a.startTime < :now")
    List<Auction> findUpcomingToActivate(@Param("status") AuctionStatus status, @Param("now") Instant now);

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller JOIN FETCH a.item WHERE a.status = :status AND a.endTime < :now")
    List<Auction> findActiveToEnd(@Param("status") AuctionStatus status, @Param("now") Instant now);
}
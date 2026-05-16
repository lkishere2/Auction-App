package com.auction.app.domains.auction.bids;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {

    @Query("SELECT b FROM Bid b JOIN FETCH b.bidder JOIN FETCH b.auction a WHERE a.id = :auctionId ORDER BY b.placedAt DESC")
    List<Bid> findByAuctionIdOrderByPlacedAtDesc(@Param("auctionId") Long auctionId);

    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(Long auctionId);

    @Query("SELECT DISTINCT b.auction.id FROM Bid b WHERE b.bidder.userId = :userId")
    List<Long> findDistinctAuctionIdsByBidderUserId(@Param("userId") Long userId);

    List<Bid> findByBidderUserId(Long userId);

    boolean existsByAuctionIdAndBidderUserId(Long auctionId, Long userId);
}
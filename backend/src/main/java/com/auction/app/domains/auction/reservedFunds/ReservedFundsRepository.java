package com.auction.app.domains.auction.reservedFunds;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservedFundsRepository extends JpaRepository<ReservedFunds, Long> {

    Optional<ReservedFunds> findByUserUserIdAndAuctionId(Long userId, Long auctionId);

    List<ReservedFunds> findByUserUserIdAndStatus(Long userId, ReservedFundStatus status);

    List<ReservedFunds> findByAuctionId(Long auctionId);
}
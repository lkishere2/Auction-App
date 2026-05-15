package com.auction.app.domains.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT tx FROM Transaction tx WHERE tx.user.id = :userId ")
    Page<Transaction> getTransactionByUserId(Pageable pageable, @Param("userId") Long userId);
}

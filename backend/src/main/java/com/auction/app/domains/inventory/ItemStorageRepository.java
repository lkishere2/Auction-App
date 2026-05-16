package com.auction.app.domains.inventory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemStorageRepository extends JpaRepository<Item, Long> {

    @Query("SELECT i FROM Item i JOIN FETCH i.owner WHERE i.owner.userId = :ownerId")
    List<Item> findByOwnerUserIdWithOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT i FROM Item i JOIN FETCH i.owner WHERE i.owner.userId = :ownerId AND i.status = :status")
    List<Item> findByOwnerUserIdAndStatusWithOwner(@Param("ownerId") Long ownerId, @Param("status") ItemStatus status);

    Optional<Item> findByIdAndOwnerUserId(Long id, Long ownerId);
}

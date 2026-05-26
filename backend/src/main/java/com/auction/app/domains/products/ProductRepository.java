package com.auction.app.domains.products;

import com.auction.app.domains.products.model.Product;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(
            value = "SELECT p FROM Product p JOIN FETCH p.tags t WHERE p.owner.id = :ownerId",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Product p WHERE p.owner.id = :ownerId"
    )
    Page<Product> findAllUserProducts(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.owner.id = :currentUserId")
    Optional<Product> findByIdAndOwnerUserId(@Param("id") Long id, @Param("currentUserId") Long currentUserId);
}
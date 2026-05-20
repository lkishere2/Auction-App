package com.auction.app.domains.products;

import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT DISTINCT p FROM Product p " +
            "JOIN FETCH p.tags t " +
            "WHERE p.owner.id = :ownerId " +
            "AND (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:tags IS NULL OR t IN :tags)")
    Page<Product> findByKeywordAndTags(@Param("ownerId") Long ownerId,
                                       @Param("keyword") String keyword,
                                       @Param("tags") Set<Tag> tags,
                                       Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.owner.id = :currentUserId")
    Optional<Product> findByIdAndOwnerUserId(@Param("id") Long id, @Param("currentUserId") Long currentUserId);
}
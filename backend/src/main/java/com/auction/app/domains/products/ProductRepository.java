package com.auction.app.domains.products;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.tags " +
            "WHERE (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:tags IS NULL OR EXISTS (SELECT 1 FROM p.tags t WHERE t IN :tags))",
            countQuery = "SELECT COUNT(p) FROM Product p " +
                    "WHERE (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:tags IS NULL OR EXISTS (SELECT 1 FROM p.tags t WHERE t IN :tags))")
    Page<Product> findByKeywordAndTags(@Param("keyword") String keyword,
                                       @Param("tags") Set<Tag> tags,
                                       Pageable pageable);

}

package com.auction.app.domains.auth.refreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);

    void deleteByUserId(Long userId);

    void deleteAllByUserId(Long userId);

    List<RefreshToken> findAllByUserId(Long userId);
}
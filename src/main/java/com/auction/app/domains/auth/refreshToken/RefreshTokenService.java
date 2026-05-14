package com.auction.app.domains.auth.refreshToken;

import com.auction.app.domains.users.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public String generateRefreshToken(User user, HttpServletRequest request) {
        String userId = String.valueOf(user.getId());

        String existingToken = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
        if (existingToken != null) {
            stringRedisTemplate.delete(REFRESH_PREFIX + userId);
        }

        refreshTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();

        RefreshToken refreshTokenObj = RefreshToken.builder()
                .token(token)
                .userId(user.getId())
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .createdAt(new Date())
                .expiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL.toMillis()))
                .build();

        refreshTokenRepository.save(refreshTokenObj);
        stringRedisTemplate.opsForValue().set(REFRESH_PREFIX + userId, token, REFRESH_TOKEN_TTL);

        return token;
    }

    public RefreshToken verifyRefreshToken(String token, HttpServletRequest request) {
        RefreshToken currentToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (currentToken.getExpiresAt().before(new Date())) {
            deleteRefreshToken(token, String.valueOf(currentToken.getUserId()));
            throw new RuntimeException("Refresh token expired");
        }

        String currentIp = request.getRemoteAddr();
        String currentAgent = request.getHeader("User-Agent");

        if (!currentToken.getIpAddress().equals(currentIp) ||
                !currentToken.getUserAgent().equals(currentAgent)) {
            throw new RuntimeException("Refresh token metadata mismatch");
        }

        return currentToken;
    }

    public void deleteRefreshToken(String token, String userId) {
        refreshTokenRepository.deleteByToken(token);
        stringRedisTemplate.delete(REFRESH_PREFIX + userId);
    }

    public void blacklistAccessToken(String jti, long remainingTtlMillis) {
        if (remainingTtlMillis <= 0) return;

        stringRedisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "true",
                Duration.ofMillis(remainingTtlMillis)
        );
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElse(null);
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti)
        );
    }
}
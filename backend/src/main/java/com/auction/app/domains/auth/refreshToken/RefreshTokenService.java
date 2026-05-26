package com.auction.app.domains.auth.refreshToken;

import com.auction.app.domains.users.users.model.User;
import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenService {

    String generateRefreshToken(User user, HttpServletRequest request);

    RefreshToken verifyRefreshToken(String token, HttpServletRequest request);

    void deleteRefreshToken(String token, String userId);

    void blacklistAccessToken(String jti, long remainingTtlMillis);

    RefreshToken findByToken(String token);

    boolean isAccessTokenBlacklisted(String jti);
}
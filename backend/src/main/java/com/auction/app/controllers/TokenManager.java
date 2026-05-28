package com.auction.app.controllers;

import com.auction.app.domains.auth.auth.AuthController;
import com.auction.app.domains.auth.auth.dtos.AuthResponse;
import com.auction.app.domains.auth.auth.dtos.RefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.Date;

@Component
public class TokenManager {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserSession userSession;

    /**
     * Attempts to refresh the current user session using the stored refresh token.
     * @return true if rotation succeeded, false if refresh token was invalid/expired.
     */
    public boolean refreshTokenSession() {
        if (userSession.getRefreshToken() == null) {
            return false;
        }

        try {
            // 1. Prepare backend payload
            RefreshRequest refreshRequest = new RefreshRequest();
            refreshRequest.setRefreshToken(userSession.getRefreshToken());

            // 2. Wrap request in our metadata proxy stub to bypass NullPointerExceptions
            HttpServletRequest mockRequest = createMockHttpServletRequest();

            // 3. Call your existing REST AuthController endpoint natively
            ResponseEntity<AuthResponse> responseEntity = authController.refresh(refreshRequest, mockRequest);
            AuthResponse response = responseEntity.getBody();

            if (response != null && response.getAccessToken() != null) {
                // 4. Silently update token storage values
                userSession.setAccessToken(response.getAccessToken());
                userSession.setRefreshToken(response.getRefreshToken());
                userSession.setTokenExpirationTime(new Date(System.currentTimeMillis() + response.getExpiresIn()));

                System.out.println("Token rotation complete: Access token refreshed successfully.");
                return true;
            }
        } catch (Exception ex) {
            System.err.println("Background token refresh failed: " + ex.getMessage());
            userSession.clear(); // Force log out user if token refresh failed completely
        }
        return false;
    }

    /**
     * Ensures an operational token session is active before any service execution layer request
     */
    public void secureExecutionGuard() {
        if (userSession.isAuthenticated() && userSession.isTokenExpired()) {
            System.out.println("Access token expiration detected. Initiating automatic background rotation...");
            refreshTokenSession();
        }
    }

    private HttpServletRequest createMockHttpServletRequest() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName()) && args.length > 0 && "User-Agent".equals(args[0])) {
                        return "JavaFX Desktop Application Client";
                    }
                    if ("getRemoteAddr".equals(method.getName())) {
                        return "127.0.0.1";
                    }
                    return null;
                }
        );
    }
}
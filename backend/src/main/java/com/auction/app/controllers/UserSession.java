package com.auction.app.controllers;

import com.auction.app.infrastructure.security.CachedUserDetails;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
@Getter
@Setter
public class UserSession {
    private String accessToken;
    private String refreshToken;
    private CachedUserDetails userDetails;
    private Date tokenExpirationTime; // Track expiration explicitly on the UI thread

    /**
     * Standard cleanup alias called by the Navbar component logic during
     * token removal and session invalidation sequences.
     */
    public void clearSession() {
        this.clear();
    }

    /**
     * Flushes out all stored active user credential tokens and state fields
     * upon command execution.
     */
    public void clear() {
        this.accessToken = null;
        this.refreshToken = null;
        this.userDetails = null;
        this.tokenExpirationTime = null;
    }

    /**
     * Checks if a user is currently authenticated within the local thread context.
     */
    public boolean isAuthenticated() {
        return this.accessToken != null;
    }

    /**
     * Checks if the active access token needs to be refreshed.
     */
    public boolean isTokenExpired() {
        if (tokenExpirationTime == null) return true;
        // Check if token expires within the next 10 seconds to account for slight latency
        return new Date(System.currentTimeMillis() + 10000).after(tokenExpirationTime);
    }
}
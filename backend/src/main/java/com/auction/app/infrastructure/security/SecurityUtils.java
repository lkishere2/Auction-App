package com.auction.app.infrastructure.security;

import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        // Handle CachedUserDetails
        if (principal instanceof CachedUserDetails cached) {
            return userRepository.findById(cached.getId())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: id=" + cached.getId()));
        }

        // Handle UserDetails fallback
        if (principal instanceof UserDetails details) {
            return userRepository.findByEmail(details.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: email=" + details.getUsername()));
        }

        throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CachedUserDetails cached) {
            return cached.getId();
        }

        if (principal instanceof UserDetails details) {
            return userRepository.findByEmail(details.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + details.getUsername()))
                    .getId();
        }

        throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
    }
}

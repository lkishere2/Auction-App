package com.auction.app.infrastructure.security;

import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, CachedUserDetails> userDetailsRedisTemplate;

    private static final String PREFIX = "user:details:";
    private static final Duration TTL = Duration.ofMinutes(30);

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String key = PREFIX + email;

        CachedUserDetails cached = userDetailsRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        CachedUserDetails cachedUser = CachedUserDetails.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .balance(user.getBalance())
                .build();

        userDetailsRedisTemplate.opsForValue().set(key, cachedUser, TTL);
        return cachedUser;
    }
}
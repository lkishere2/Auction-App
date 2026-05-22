package com.auction.app.domains.auth.auth.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class AuthCacheAdapter implements AuthRedisPort {

    private final StringRedisTemplate redisTemplate;

    private static final String VERIFY_EMAIL_PREFIX = "auth:verify:email:";
    private static final String RESET_CODE_PREFIX = "auth:reset:code:";
    private static final String RESET_TICKET_PREFIX = "auth:reset:verified:";

    private static final long CODE_TTL_MINUTES = 15;
    private static final long TICKET_TTL_MINUTES = 5;

    public AuthCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveEmailVerificationCode(String email, String code) {
        redisTemplate.opsForValue().set(VERIFY_EMAIL_PREFIX + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public String getEmailVerificationCode(String email) {
        return redisTemplate.opsForValue().get(VERIFY_EMAIL_PREFIX + email);
    }

    @Override
    public void deleteEmailVerificationCode(String email) {
        redisTemplate.delete(VERIFY_EMAIL_PREFIX + email);
    }

    @Override
    public void savePasswordResetCode(String email, String code) {
        redisTemplate.opsForValue().set(RESET_CODE_PREFIX + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public String getPasswordResetCode(String email) {
        return redisTemplate.opsForValue().get(RESET_CODE_PREFIX + email);
    }

    @Override
    public void deletePasswordResetCode(String email) {
        redisTemplate.delete(RESET_CODE_PREFIX + email);
    }

    @Override
    public void savePasswordResetVerifiedTicket(String email) {
        redisTemplate.opsForValue().set(RESET_TICKET_PREFIX + email, "true", TICKET_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public boolean hasValidPasswordResetTicket(String email) {
        String value = redisTemplate.opsForValue().get(RESET_TICKET_PREFIX + email);
        return "true".equals(value);
    }

    @Override
    public void deletePasswordResetVerifiedTicket(String email) {
        redisTemplate.delete(RESET_TICKET_PREFIX + email);
    }
}
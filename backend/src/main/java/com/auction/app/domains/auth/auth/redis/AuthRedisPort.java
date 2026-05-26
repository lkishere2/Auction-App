package com.auction.app.domains.auth.auth.redis;

public interface AuthRedisPort {

    // Email registration
    void saveEmailVerificationCode(String email, String code);
    String getEmailVerificationCode(String email);
    void deleteEmailVerificationCode(String email);

    // Password reset
    void savePasswordResetCode(String email, String code);
    String getPasswordResetCode(String email);
    void deletePasswordResetCode(String email);

    void savePasswordResetVerifiedTicket(String email);
    boolean hasValidPasswordResetTicket(String email);
    void deletePasswordResetVerifiedTicket(String email);

}
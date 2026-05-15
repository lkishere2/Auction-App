package com.auction.app.domains.auth.auth;

import com.auction.app.domains.auth.email.VerifyRequest;
import com.auction.app.domains.users.User;

public interface AuthService {
    User register(RegisterRequest registerRequest);
    User login(LoginRequest loginRequest);
    void verifyUser(VerifyRequest verifyRequest);
    void resendVerificationCode(String email);
    void sendVerificationEmail(User user);
    void requestPasswordReset(String email);
    void verifyPasswordReset(VerifyRequest verifyRequest);
    User resetPassword(String email, String password);
}

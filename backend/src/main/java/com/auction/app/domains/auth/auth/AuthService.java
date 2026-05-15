package com.auction.app.domains.auth.auth;

import com.auction.app.domains.users.users.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    void register(RegisterRequest request, HttpServletRequest httpRequest);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    AuthResponse refresh(String refreshToken, HttpServletRequest request);
    void logout(HttpServletRequest request);
    void verifyUser(VerifyRequest verifyRequest);
    void resendVerificationCode(String email);
    void sendVerificationEmail(User user);
    void requestPasswordReset(String email);
    void verifyPasswordReset(VerifyRequest verifyRequest);
    void resetPassword(String email, String password);
}
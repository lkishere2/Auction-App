package com.auction.app.domains.auth.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest) {
        authService.register(request, httpRequest);
        return ResponseEntity.ok("Send verification code for user!");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok("Logout successfully!");
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken(), httpRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody @Valid VerifyRequest request) {
        authService.verifyUser(request);
        return ResponseEntity.ok("Verify successfully!");
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<String> resendVerificationCode(@RequestBody @Valid EmailRequest request) {
        authService.resendVerificationCode(request.getEmail());
        return ResponseEntity.ok("Send verification code for user!");
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody @Valid EmailRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok("Send password reset verification code for user!");
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<String> verifyPasswordReset(@RequestBody @Valid VerifyRequest request) {
        authService.verifyPasswordReset(request);
        return ResponseEntity.ok("Verify successfully!");
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Reset successfully!");
    }
}
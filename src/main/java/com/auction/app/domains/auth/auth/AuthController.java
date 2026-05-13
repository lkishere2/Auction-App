package com.auction.app.domains.auth.auth;

import com.auction.app.domains.auth.email.VerifyRequest;
import com.auction.app.domains.users.User;
import com.auction.app.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping(path = "/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest registerRequest) {
        User registeredUser = authService.register(registerRequest);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping(path = "/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        User authenticatedUser = authService.login(loginRequest);
        String token = jwtService.generateToken(authenticatedUser);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        authResponse.setExpiresIn(jwtService.getExpirationTime());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping(path = "/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyRequest verifyRequest) {
        try {
            authService.verifyUser(verifyRequest);
            return ResponseEntity.ok("Account verified successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(path = "/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code resent");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(path = "/send/password-reset")
    public ResponseEntity<?> sendPasswordResetRequest(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            authService.requestPasswordReset(email);
            return ResponseEntity.ok("Password reset request sent");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(path = "/verify/password-reset")
    public ResponseEntity<?> verifyPasswordResetRequest(@RequestBody VerifyRequest verifyRequest) {
        try {
            authService.verifyPasswordReset(verifyRequest);
            return ResponseEntity.ok("Password reset verified successfully");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(path = "/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");
            User user = authService.resetPassword(email, password);
            return ResponseEntity.ok(user);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

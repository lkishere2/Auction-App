package com.auction.app.domains.auth.auth;

import com.auction.app.domains.auth.auth.dtos.AuthResponse;
import com.auction.app.domains.auth.auth.dtos.LoginRequest;
import com.auction.app.domains.auth.auth.dtos.RegisterRequest;
import com.auction.app.domains.auth.auth.dtos.VerifyRequest;
import com.auction.app.domains.auth.auth.exceptions.*;
import com.auction.app.domains.auth.email.EmailService;
import com.auction.app.domains.auth.refreshToken.RefreshToken;
import com.auction.app.domains.auth.refreshToken.RefreshTokenService;
import com.auction.app.domains.users.users.Provider;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import com.auction.app.infrastructure.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (user.isEnabled()) {
                return;
            }

            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));
            user.setRequestPasswordReset(false);
            user.setPasswordResetVerified(false);
            userRepository.save(user);
            sendVerificationEmail(user);
            return;
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .provider(Provider.LOCAL)
                .verificationCode(generateVerificationCode())
                .verificationExpiration(LocalDateTime.now().plusMinutes(15))
                .enabled(false)
                .build();

        userRepository.save(newUser);
        sendVerificationEmail(newUser);
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user, httpRequest);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    @Override
    public AuthResponse refresh(String refreshToken, HttpServletRequest request) {
        RefreshToken currentToken = refreshTokenService.verifyRefreshToken(refreshToken, request);

        User user = userRepository.findById(currentToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokenService.deleteRefreshToken(refreshToken, String.valueOf(user.getId()));

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.generateRefreshToken(user, request);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    @Override
    public void logout(HttpServletRequest request) {
        String accessToken = extractBearerToken(request);
        String refreshToken = request.getHeader("X-Refresh-Token");

        if (refreshToken != null) {
            RefreshToken current = refreshTokenService.findByToken(refreshToken);
            if (current != null) {
                refreshTokenService.deleteRefreshToken(
                        refreshToken,
                        String.valueOf(current.getUserId()));
            }
        }

        if (accessToken != null) {
            String jti = jwtService.extractJti(accessToken);
            long remainingTtl = jwtService.getRemainingTtlMillis(accessToken);
            refreshTokenService.blacklistAccessToken(jti, remainingTtl);
        }
    }

    @Override
    public void verifyUser(VerifyRequest verifyRequest) {
        User user = userRepository.findByEmail(verifyRequest.getEmail()).orElse(null);

        if (user == null || user.isEnabled() || !user.getVerificationCode().equals(verifyRequest.getVerificationCode())) {
            throw new InvalidVerificationCodeException("The verification code is invalid or has expired.");
        }

        if (user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
            throw new VerificationCodeExpiredException("Verification code expired");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationExpiration(null);
        userRepository.save(user);
    }

    @Override
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || user.isEnabled()) {
            return;
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        sendVerificationEmail(user);
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.isEnabled()) {
            return;
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));
        user.setRequestPasswordReset(true);
        userRepository.save(user);
        sendVerificationEmail(user);
    }

    @Override
    public void verifyPasswordReset(VerifyRequest verifyRequest) {
        User user = userRepository.findByEmail(verifyRequest.getEmail()).orElse(null);

        if (user == null || !user.isRequestPasswordReset() || !user.getVerificationCode().equals(verifyRequest.getVerificationCode())) {
            throw new InvalidVerificationCodeException("Invalid or expired verification code.");
        }

        if (user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
            throw new VerificationCodeExpiredException("Verification code expired");
        }

        user.setVerificationCode(null);
        user.setVerificationExpiration(null);
        user.setPasswordResetVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.isPasswordResetVerified()) {
            throw new InvalidPasswordResetFlowException("Invalid password reset request.");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordResetVerified(false);
        user.setRequestPasswordReset(false);
        userRepository.save(user);
    }

    @Override
    public void sendVerificationEmail(User user) {
        String subject = "Account verification code";
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + user.getVerificationCode()
                + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationMail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            throw new EmailSendFailureException("Failed to send verification email");
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
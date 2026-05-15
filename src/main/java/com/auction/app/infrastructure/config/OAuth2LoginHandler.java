package com.auction.app.infrastructure.config;

import com.auction.app.domains.auth.refreshToken.RefreshTokenService;
import com.auction.app.domains.users.users.Provider;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import com.auction.app.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2LoginHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        if (oAuth2User == null) {
            throw new RuntimeException("Something went wrong");
        }

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            // Inline registerOAuth2 here to avoid circular dependency:
            // AuthServiceImpl -> AuthenticationManager -> SecurityConfig -> OAuth2LoginHandler -> AuthServiceImpl
            User newUser = User.builder()
                    .username(name)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .enabled(true)
                    .provider(Provider.GOOGLE)
                    .verificationCode(null)
                    .verificationExpiration(null)
                    .build();
            userRepository.save(newUser);
            optionalUser = userRepository.findByEmail(email);
        }

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String accessToken = jwtService.generateToken(user);
            String refreshToken = refreshTokenService.generateRefreshToken(user, request);

            String targetUrl = String.format(
                    "%s?access_token=%s&refresh_token=%s",
                    redirectUri,
                    accessToken,
                    refreshToken
            );
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User registration failed");
        }
    }
}
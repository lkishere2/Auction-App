package com.auction.app.auth;

import com.auction.app.domains.auth.auth.AuthServiceImpl;
import com.auction.app.domains.auth.auth.dtos.AuthResponse;
import com.auction.app.domains.auth.auth.dtos.LoginRequest;
import com.auction.app.domains.auth.auth.dtos.RegisterRequest;
import com.auction.app.domains.auth.auth.dtos.VerifyRequest;
import com.auction.app.domains.auth.exceptions.EmailSendFailureException;
import com.auction.app.domains.auth.exceptions.InvalidPasswordResetFlowException;
import com.auction.app.domains.auth.exceptions.InvalidVerificationCodeException;
import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.domains.auth.auth.redis.AuthRedisPort;
import com.auction.app.domains.auth.email.EmailService;
import com.auction.app.domains.auth.refreshToken.RefreshToken;
import com.auction.app.domains.auth.refreshToken.RefreshTokenService;
import com.auction.app.domains.users.users.model.Provider;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import com.auction.app.infrastructure.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuthRedisPort cache;
    @Mock HttpServletRequest httpRequest;

    @InjectMocks
    AuthServiceImpl authService;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User enabledUser() {
        return User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("encoded-password")
                .provider(Provider.LOCAL)
                .enabled(true)
                .build();
    }

    private User unverifiedUser() {
        return User.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .password("encoded-password")
                .provider(Provider.LOCAL)
                .enabled(false)
                .build();
    }

    // ==================================================================
    // register()
    // ==================================================================

    @Nested
    class Register {

        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setUsername("newuser");
            request.setPassword("password123");
        }

        @Test
        void newUser_savesAndSendsVerificationEmail() throws MessagingException {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");

            authService.register(request, httpRequest);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(request.getEmail());
            assertThat(saved.getDisplayName()).isEqualTo(request.getUsername());
            assertThat(saved.getPassword()).isEqualTo("encoded");
            assertThat(saved.isEnabled()).isFalse();
            assertThat(saved.getProvider()).isEqualTo(Provider.LOCAL);

            verify(cache).saveEmailVerificationCode(eq(request.getEmail()), anyString());
            verify(emailService).sendVerificationMail(eq(request.getEmail()), anyString(), anyString());
        }

        @Test
        void existingEnabledUser_doesNothing() throws MessagingException {
            when(userRepository.findByEmail(request.getEmail()))
                    .thenReturn(Optional.of(enabledUser()));

            authService.register(request, httpRequest);

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationMail(any(), any(), any());
        }

        @Test
        void existingUnverifiedUser_updatesCredentialsAndResends() throws MessagingException {
            User unverified = unverifiedUser();
            unverified.setEmail(request.getEmail());
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(unverified));
            when(passwordEncoder.encode(request.getPassword())).thenReturn("new-encoded");

            authService.register(request, httpRequest);

            verify(userRepository).save(unverified);
            assertThat(unverified.getDisplayName()).isEqualTo(request.getUsername());
            assertThat(unverified.getPassword()).isEqualTo("new-encoded");

            verify(cache).saveEmailVerificationCode(eq(request.getEmail()), anyString());
            verify(emailService).sendVerificationMail(eq(request.getEmail()), anyString(), anyString());
        }

        @Test
        void generatedCode_isSixDigits() throws MessagingException {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");

            authService.register(request, httpRequest);

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(cache).saveEmailVerificationCode(eq(request.getEmail()), codeCaptor.capture());

            String code = codeCaptor.getValue();
            assertThat(code).matches("\\d{6}");
            assertThat(Integer.parseInt(code)).isBetween(100000, 999999);
        }

        @Test
        void emailSendFailure_throwsEmailSendFailureException() throws MessagingException {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            doThrow(new MessagingException("SMTP down"))
                    .when(emailService).sendVerificationMail(any(), any(), any());

            assertThatThrownBy(() -> authService.register(request, httpRequest))
                    .isInstanceOf(EmailSendFailureException.class);
        }
    }

    // ==================================================================
    // login()
    // ==================================================================

    @Nested
    class Login {

        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = new LoginRequest();
            request.setEmail("alice@example.com");
            request.setPassword("password123");
        }

        @Test
        void validCredentials_returnsAuthResponse() {
            User user = enabledUser();
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("access-token");
            when(jwtService.getExpirationTime()).thenReturn(3600L);
            when(refreshTokenService.generateRefreshToken(user, httpRequest)).thenReturn("refresh-token");

            AuthResponse response = authService.login(request, httpRequest);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
        }

        @Test
        void validCredentials_callsAuthenticationManager() {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(enabledUser()));
            when(jwtService.generateToken(any())).thenReturn("token");
            when(refreshTokenService.generateRefreshToken(any(), any())).thenReturn("refresh");

            authService.login(request, httpRequest);

            verify(authenticationManager).authenticate(
                    argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken
                            && auth.getPrincipal().equals(request.getEmail())
                            && auth.getCredentials().equals(request.getPassword()))
            );
        }

        @Test
        void authManagerThrows_propagatesBadCredentialsException() {
            doThrow(new BadCredentialsException("bad"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void userNotFound_throwsBadCredentialsException() {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        void disabledUser_throwsBadCredentialsException() {
            when(userRepository.findByEmail(request.getEmail()))
                    .thenReturn(Optional.of(unverifiedUser()));

            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void disabledUser_doesNotGenerateTokens() {
            when(userRepository.findByEmail(request.getEmail()))
                    .thenReturn(Optional.of(unverifiedUser()));

            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenService, never()).generateRefreshToken(any(), any());
        }
    }

    // ==================================================================
    // refresh()
    // ==================================================================

    @Nested
    class Refresh {

        @Test
        void validRefreshToken_returnsNewTokenPair() {
            User user = enabledUser();
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUserId(user.getId());
            refreshToken.setToken("old-refresh");

            when(refreshTokenService.verifyRefreshToken("old-refresh", httpRequest)).thenReturn(refreshToken);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("new-access");
            when(jwtService.getExpirationTime()).thenReturn(3600L);
            when(refreshTokenService.generateRefreshToken(user, httpRequest)).thenReturn("new-refresh");

            AuthResponse response = authService.refresh("old-refresh", httpRequest);

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        }

        @Test
        void validRefreshToken_deletesOldToken() {
            User user = enabledUser();
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUserId(user.getId());
            refreshToken.setToken("old-refresh");

            when(refreshTokenService.verifyRefreshToken("old-refresh", httpRequest)).thenReturn(refreshToken);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("new-access");
            when(refreshTokenService.generateRefreshToken(user, httpRequest)).thenReturn("new-refresh");

            authService.refresh("old-refresh", httpRequest);

            verify(refreshTokenService).deleteRefreshToken("old-refresh", String.valueOf(user.getId()));
        }

        @Test
        void userNotFound_throwsUserNotFoundException() {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUserId(99L);
            refreshToken.setToken("some-token");

            when(refreshTokenService.verifyRefreshToken("some-token", httpRequest)).thenReturn(refreshToken);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("some-token", httpRequest))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================================================================
    // logout()
    // ==================================================================

    @Nested
    class Logout {

        @Test
        void withBothTokens_blacklistsAccessAndDeletesRefresh() {
            User user = enabledUser();
            RefreshToken storedRefresh = new RefreshToken();
            storedRefresh.setToken("refresh-token");
            storedRefresh.setUserId(user.getId());

            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer access-token");
            when(httpRequest.getHeader("X-Refresh-Token")).thenReturn("refresh-token");
            when(refreshTokenService.findByToken("refresh-token")).thenReturn(storedRefresh);
            when(jwtService.extractJti("access-token")).thenReturn("jti-123");
            when(jwtService.getRemainingTtlMillis("access-token")).thenReturn(60000L);

            authService.logout(httpRequest);

            verify(refreshTokenService).deleteRefreshToken("refresh-token", String.valueOf(user.getId()));
            verify(refreshTokenService).blacklistAccessToken("jti-123", 60000L);
        }

        @Test
        void withNoRefreshToken_onlyBlacklistsAccessToken() {
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer access-token");
            when(httpRequest.getHeader("X-Refresh-Token")).thenReturn(null);
            when(jwtService.extractJti("access-token")).thenReturn("jti-123");
            when(jwtService.getRemainingTtlMillis("access-token")).thenReturn(60000L);

            authService.logout(httpRequest);

            verify(refreshTokenService, never()).deleteRefreshToken(any(), any());
            verify(refreshTokenService).blacklistAccessToken("jti-123", 60000L);
        }

        @Test
        void withNoAuthorizationHeader_doesNotBlacklistAccessToken() {
            when(httpRequest.getHeader("Authorization")).thenReturn(null);
            when(httpRequest.getHeader("X-Refresh-Token")).thenReturn(null);

            authService.logout(httpRequest);

            verify(refreshTokenService, never()).blacklistAccessToken(any(), anyLong());
        }

        @Test
        void refreshTokenNotFound_skipsRefreshDeletion() {
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer access-token");
            when(httpRequest.getHeader("X-Refresh-Token")).thenReturn("ghost-token");
            when(refreshTokenService.findByToken("ghost-token")).thenReturn(null);
            when(jwtService.extractJti("access-token")).thenReturn("jti-123");
            when(jwtService.getRemainingTtlMillis("access-token")).thenReturn(60000L);

            authService.logout(httpRequest);

            verify(refreshTokenService, never()).deleteRefreshToken(any(), any());
            verify(refreshTokenService).blacklistAccessToken("jti-123", 60000L);
        }
    }

    // ==================================================================
    // verifyUser()
    // ==================================================================

    @Nested
    class VerifyUser {

        private VerifyRequest verifyRequest;

        @BeforeEach
        void setUp() {
            verifyRequest = new VerifyRequest();
            verifyRequest.setEmail("bob@example.com");
            verifyRequest.setVerificationCode("123456");
        }

        @Test
        void validCode_enablesUserAndClearsCache() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));
            when(cache.getEmailVerificationCode(verifyRequest.getEmail())).thenReturn("123456");

            authService.verifyUser(verifyRequest);

            assertThat(user.isEnabled()).isTrue();
            verify(userRepository).save(user);
            verify(cache).deleteEmailVerificationCode(verifyRequest.getEmail());
        }

        @Test
        void wrongCode_throwsInvalidVerificationCodeException() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));
            when(cache.getEmailVerificationCode(verifyRequest.getEmail())).thenReturn("999999");

            assertThatThrownBy(() -> authService.verifyUser(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        void expiredCode_throwsInvalidVerificationCodeException() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));
            when(cache.getEmailVerificationCode(verifyRequest.getEmail())).thenReturn(null);

            assertThatThrownBy(() -> authService.verifyUser(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        void userNotFound_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.empty());
            when(cache.getEmailVerificationCode(verifyRequest.getEmail())).thenReturn("123456");

            assertThatThrownBy(() -> authService.verifyUser(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        void alreadyEnabledUser_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(verifyRequest.getEmail()))
                    .thenReturn(Optional.of(enabledUser()));
            when(cache.getEmailVerificationCode(verifyRequest.getEmail())).thenReturn("123456");

            assertThatThrownBy(() -> authService.verifyUser(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }
    }

    // ==================================================================
    // resendVerificationCode()
    // ==================================================================

    @Nested
    class ResendVerificationCode {

        @Test
        void unverifiedUser_savesNewCodeAndSendsEmail() throws MessagingException {
            User user = unverifiedUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            authService.resendVerificationCode(user.getEmail());

            verify(cache).saveEmailVerificationCode(eq(user.getEmail()), anyString());
            verify(emailService).sendVerificationMail(eq(user.getEmail()), anyString(), anyString());
        }

        @Test
        void userNotFound_doesNothing() throws MessagingException {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            authService.resendVerificationCode("ghost@example.com");

            verify(cache, never()).saveEmailVerificationCode(any(), any());
            verify(emailService, never()).sendVerificationMail(any(), any(), any());
        }

        @Test
        void alreadyEnabledUser_doesNothing() throws MessagingException {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(enabledUser()));

            authService.resendVerificationCode("alice@example.com");

            verify(cache, never()).saveEmailVerificationCode(any(), any());
            verify(emailService, never()).sendVerificationMail(any(), any(), any());
        }
    }

    // ==================================================================
    // requestPasswordReset()
    // ==================================================================

    @Nested
    class RequestPasswordReset {

        @Test
        void enabledUser_savesCodeAndSendsEmail() throws MessagingException {
            User user = enabledUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            authService.requestPasswordReset(user.getEmail());

            verify(cache).savePasswordResetCode(eq(user.getEmail()), anyString());
            verify(emailService).sendVerificationMail(eq(user.getEmail()), anyString(), anyString());
        }

        @Test
        void userNotFound_doesNothing() throws MessagingException {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            authService.requestPasswordReset("ghost@example.com");

            verify(cache, never()).savePasswordResetCode(any(), any());
            verify(emailService, never()).sendVerificationMail(any(), any(), any());
        }

        @Test
        void unverifiedUser_doesNothing() throws MessagingException {
            when(userRepository.findByEmail("bob@example.com"))
                    .thenReturn(Optional.of(unverifiedUser()));

            authService.requestPasswordReset("bob@example.com");

            verify(cache, never()).savePasswordResetCode(any(), any());
            verify(emailService, never()).sendVerificationMail(any(), any(), any());
        }
    }

    // ==================================================================
    // verifyPasswordReset()
    // ==================================================================

    @Nested
    class VerifyPasswordReset {

        private VerifyRequest verifyRequest;

        @BeforeEach
        void setUp() {
            verifyRequest = new VerifyRequest();
            verifyRequest.setEmail("alice@example.com");
            verifyRequest.setVerificationCode("123456");
        }

        @Test
        void validCode_clearsResetCodeAndSavesTicket() {
            when(userRepository.findByEmail(verifyRequest.getEmail()))
                    .thenReturn(Optional.of(enabledUser()));
            when(cache.getPasswordResetCode(verifyRequest.getEmail())).thenReturn("123456");

            authService.verifyPasswordReset(verifyRequest);

            verify(cache).deletePasswordResetCode(verifyRequest.getEmail());
            verify(cache).savePasswordResetVerifiedTicket(verifyRequest.getEmail());
        }

        @Test
        void wrongCode_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(verifyRequest.getEmail()))
                    .thenReturn(Optional.of(enabledUser()));
            when(cache.getPasswordResetCode(verifyRequest.getEmail())).thenReturn("999999");

            assertThatThrownBy(() -> authService.verifyPasswordReset(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        void expiredCode_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(verifyRequest.getEmail()))
                    .thenReturn(Optional.of(enabledUser()));
            when(cache.getPasswordResetCode(verifyRequest.getEmail())).thenReturn(null);

            assertThatThrownBy(() -> authService.verifyPasswordReset(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        void userNotFound_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.empty());
            when(cache.getPasswordResetCode(verifyRequest.getEmail())).thenReturn("123456");

            assertThatThrownBy(() -> authService.verifyPasswordReset(verifyRequest))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }
    }

    // ==================================================================
    // resetPassword()
    // ==================================================================

    @Nested
    class ResetPassword {

        @Test
        void validTicket_updatesPasswordAndClearsTicket() {
            User user = enabledUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(cache.hasValidPasswordResetTicket(user.getEmail())).thenReturn(true);
            when(passwordEncoder.encode("newpassword")).thenReturn("encoded-new");

            authService.resetPassword(user.getEmail(), "newpassword");

            assertThat(user.getPassword()).isEqualTo("encoded-new");
            verify(userRepository).save(user);
            verify(cache).deletePasswordResetVerifiedTicket(user.getEmail());
        }

        @Test
        void noVerifiedTicket_throwsInvalidPasswordResetFlowException() {
            User user = enabledUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(cache.hasValidPasswordResetTicket(user.getEmail())).thenReturn(false);

            assertThatThrownBy(() -> authService.resetPassword(user.getEmail(), "newpassword"))
                    .isInstanceOf(InvalidPasswordResetFlowException.class);
        }

        @Test
        void userNotFound_throwsInvalidPasswordResetFlowException() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
            when(cache.hasValidPasswordResetTicket("ghost@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.resetPassword("ghost@example.com", "newpassword"))
                    .isInstanceOf(InvalidPasswordResetFlowException.class);
        }

        @Test
        void validTicket_passwordIsEncoded() {
            User user = enabledUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(cache.hasValidPasswordResetTicket(user.getEmail())).thenReturn(true);
            when(passwordEncoder.encode("newpassword")).thenReturn("encoded-new");

            authService.resetPassword(user.getEmail(), "newpassword");

            verify(passwordEncoder).encode("newpassword");
            assertThat(user.getPassword()).isNotEqualTo("newpassword");
        }
    }
}
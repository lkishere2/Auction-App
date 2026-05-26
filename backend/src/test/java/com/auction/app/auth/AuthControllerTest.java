package com.auction.app.auth;

import com.auction.app.domains.auth.auth.AuthController;
import com.auction.app.domains.auth.auth.AuthService;
import com.auction.app.domains.auth.auth.dtos.AuthResponse;
import com.auction.app.domains.auth.exceptions.EmailSendFailureException;
import com.auction.app.domains.auth.exceptions.InvalidPasswordResetFlowException;
import com.auction.app.domains.auth.exceptions.InvalidVerificationCodeException;
import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.infrastructure.exception.GlobalExceptionHandler;
import com.auction.app.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthService authService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean AuthenticationProvider authenticationProvider;

    private static final String BASE = "/api/v1/auth";

    private String json(Map<String, String> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ==================================================================
    // POST /register
    // ==================================================================

    @Nested
    class Register {

        @Test
        void validRequest_returns200WithMessage() throws Exception {
            doNothing().when(authService).register(any(), any());

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "alice",
                                    "email", "alice@example.com",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Verification code has been sent!"));
        }

        @Test
        void missingUsername_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void blankUsername_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "",
                                    "email", "alice@example.com",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "alice",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "alice",
                                    "email", "not-an-email",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void passwordTooShort_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "alice",
                                    "email", "alice@example.com",
                                    "password", "abc"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void passwordTooLong_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "alice",
                                    "email", "alice@example.com",
                                    "password", "thispasswordiswaytoolong"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void emailSendFailure_returns500() throws Exception {
            doThrow(new EmailSendFailureException("SMTP error"))
                    .when(authService).register(any(), any());

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "alice",
                                    "email", "alice@example.com",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================================================================
    // POST /login
    // ==================================================================

    @Nested
    class Login {

        private final AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .build();

        @Test
        void validCredentials_returns200WithTokens() throws Exception {
            when(authService.login(any(), any())).thenReturn(authResponse);

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("password", "secret123"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "not-an-email",
                                    "password", "secret123"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void missingPassword_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "alice@example.com"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void passwordTooShort_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "abc"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void passwordTooLong_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "thispasswordiswaytoolong"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void badCredentials_returns401() throws Exception {
            when(authService.login(any(), any()))
                    .thenThrow(new BadCredentialsException("Invalid email or password"));

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "wrongpass"
                            ))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================================================================
    // POST /logout
    // ==================================================================

    @Nested
    class Logout {

        @Test
        void validRequest_returns200() throws Exception {
            doNothing().when(authService).logout(any());

            mockMvc.perform(post(BASE + "/logout"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Logout successfully!"));
        }

        @Test
        void serviceThrows_propagatesError() throws Exception {
            doThrow(new UserNotFoundException("User not found"))
                    .when(authService).logout(any());

            mockMvc.perform(post(BASE + "/logout"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================================================================
    // POST /refresh
    // ==================================================================

    @Nested
    class Refresh {

        private final AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .expiresIn(3600L)
                .build();

        @Test
        void validToken_returns200WithNewTokenPair() throws Exception {
            when(authService.refresh(anyString(), any())).thenReturn(authResponse);

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", "a-valid-refresh-token-here"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        }

        @Test
        void missingRefreshToken_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void blankRefreshToken_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void tokenTooShort_returns400() throws Exception {
            // @Size(min = 10)
            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", "short"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidToken_returns401() throws Exception {
            when(authService.refresh(anyString(), any()))
                    .thenThrow(new BadCredentialsException("Invalid token"));

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", "a-valid-refresh-token-here"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void userNotFound_returns404() throws Exception {
            when(authService.refresh(anyString(), any()))
                    .thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", "a-valid-refresh-token-here"))))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================================================================
    // POST /verify
    // ==================================================================

    @Nested
    class Verify {

        @Test
        void validRequest_returns200() throws Exception {
            doNothing().when(authService).verifyUser(any());

            mockMvc.perform(post(BASE + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "123456"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Verify successfully!"));
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("verificationCode", "123456"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "not-an-email",
                                    "verificationCode", "123456"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void codeBelowRange_returns400() throws Exception {
            // @Range(min = 100000)
            mockMvc.perform(post(BASE + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "99999"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void codeAboveRange_returns400() throws Exception {
            // @Range(max = 999999)
            mockMvc.perform(post(BASE + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "1000000"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidCode_returns400() throws Exception {
            doThrow(new InvalidVerificationCodeException("Invalid or expired code"))
                    .when(authService).verifyUser(any());

            mockMvc.perform(post(BASE + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "123456"
                            ))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================================================================
    // POST /verify/resend
    // ==================================================================

    @Nested
    class ResendVerification {

        @Test
        void validEmail_returns200() throws Exception {
            doNothing().when(authService).resendVerificationCode(anyString());

            mockMvc.perform(post(BASE + "/verify/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "alice@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Verification code has been sent!"));
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/verify/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void blankEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/verify/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/verify/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "not-an-email"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void emailSendFailure_returns500() throws Exception {
            doThrow(new EmailSendFailureException("SMTP error"))
                    .when(authService).resendVerificationCode(anyString());

            mockMvc.perform(post(BASE + "/verify/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "alice@example.com"))))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================================================================
    // POST /password-reset/request
    // ==================================================================

    @Nested
    class PasswordResetRequest {

        @Test
        void validEmail_returns200() throws Exception {
            doNothing().when(authService).requestPasswordReset(anyString());

            mockMvc.perform(post(BASE + "/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "alice@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Password reset verification code has been sent!"));
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "not-an-email"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void emailSendFailure_returns500() throws Exception {
            doThrow(new EmailSendFailureException("SMTP error"))
                    .when(authService).requestPasswordReset(anyString());

            mockMvc.perform(post(BASE + "/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "alice@example.com"))))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================================================================
    // POST /password-reset/verify
    // ==================================================================

    @Nested
    class PasswordResetVerify {

        @Test
        void validRequest_returns200() throws Exception {
            doNothing().when(authService).verifyPasswordReset(any());

            mockMvc.perform(post(BASE + "/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "123456"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Verify successfully!"));
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("verificationCode", "123456"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "not-an-email",
                                    "verificationCode", "123456"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void codeBelowRange_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "99999"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void codeAboveRange_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "1000000"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidCode_returns400() throws Exception {
            doThrow(new InvalidVerificationCodeException("Invalid or expired code"))
                    .when(authService).verifyPasswordReset(any());

            mockMvc.perform(post(BASE + "/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "verificationCode", "123456"
                            ))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================================================================
    // POST /password-reset/confirm
    // ==================================================================

    @Nested
    class PasswordResetConfirm {

        @Test
        void validRequest_returns200() throws Exception {
            doNothing().when(authService).resetPassword(anyString(), anyString());

            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "newpassword"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Reset successfully!"));
        }

        @Test
        void missingEmail_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("password", "newpassword"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "not-an-email",
                                    "password", "newpassword"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void missingPassword_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "alice@example.com"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void passwordTooShort_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "abc"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void passwordTooLong_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "thispasswordiswaytoolong"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidResetFlow_returns400() throws Exception {
            doThrow(new InvalidPasswordResetFlowException("Invalid reset request"))
                    .when(authService).resetPassword(anyString(), anyString());

            mockMvc.perform(post(BASE + "/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "alice@example.com",
                                    "password", "newpassword"
                            ))))
                    .andExpect(status().isBadRequest());
        }
    }
}
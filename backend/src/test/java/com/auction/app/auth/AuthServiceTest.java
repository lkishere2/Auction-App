package com.auction.app.auth;

import com.auction.app.domains.auth.auth.AuthServiceImpl;
import com.auction.app.domains.auth.auth.dtos.RegisterRequest;
import com.auction.app.domains.auth.auth.dtos.VerifyRequest;
import com.auction.app.domains.auth.email.EmailService;
import com.auction.app.domains.users.users.Provider;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ==========================================
    // 1) HAPPY PATH
    // ==========================================
    @Test
    void register_HappyPath_FreshUser_ShouldSaveAndSendEmail() throws MessagingException {
        RegisterRequest request = createValidRegisterRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        authService.register(request, httpRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser);
        assertEquals(request.getUsername(), savedUser.getDisplayName());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(request.getEmail(), savedUser.getEmail());
        assertEquals(Provider.LOCAL, savedUser.getProvider());
        assertFalse(savedUser.isEnabled());
        assertNotNull(savedUser.getVerificationCode());
        assertTrue(savedUser.getVerificationExpiration().isAfter(LocalDateTime.now()));
        verify(emailService, times(1)).sendVerificationMail(eq(request.getEmail()), any(), any());
    }

    // ==========================================
    // 2) VALIDATION TESTS (INPUT/OUTPUT)
    // ==========================================

    // Username: 2 Tests
    @Test
    void validation_UsernameEmpty_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setUsername("");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_UsernameNull_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setUsername(null);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // Email: 3 Tests
    @Test
    void validation_EmailEmpty_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setEmail("");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_EmailNull_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setEmail(null);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_EmailInvalidFormat_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setEmail("not-an-email");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // Password: 4 Tests
    @Test
    void validation_PasswordEmpty_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setPassword("");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_PasswordNull_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setPassword(null);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_PasswordTooShort_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setPassword("12345"); // 5 chars
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_PasswordTooLong_ShouldFail() {
        RegisterRequest request = createValidRegisterRequest();
        request.setPassword("1234567890123456"); // 16 chars
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ==========================================
    // 3) LOGIC TESTS
    // ==========================================
    @Test
    void register_AccountAlreadyVerified_ShouldThrowException() {
        RegisterRequest request = createValidRegisterRequest();
        User existingUser = User.builder()
                .email(request.getEmail())
                .enabled(true)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.register(request, httpRequest));

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_AccountRegisteredButNotVerified_ShouldRecycleAccount() throws MessagingException {
        RegisterRequest request = createValidRegisterRequest();
        User existingUnverifiedUser = User.builder()
                .username("oldUsername")
                .password("oldPassword")
                .email(request.getEmail())
                .enabled(false)
                .requestPasswordReset(true)
                .passwordResetVerified(true)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUnverifiedUser));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("newEncodedPassword");

        authService.register(request, httpRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();

        assertEquals(request.getUsername(), updatedUser.getDisplayName());
        assertEquals("newEncodedPassword", updatedUser.getPassword());
        assertFalse(updatedUser.isEnabled());
        assertFalse(updatedUser.isRequestPasswordReset());
        assertFalse(updatedUser.isPasswordResetVerified());
        assertNotNull(updatedUser.getVerificationCode());
        verify(emailService).sendVerificationMail(eq(request.getEmail()), any(), any());
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validUser");
        request.setEmail("valid@example.com");
        request.setPassword("secure123");
        return request;
    }

    // ==========================================
    // 4) VERIFY USER TESTS
    // ==========================================

    // --- Happy Path ---
    @Test
    void verifyUser_HappyPath_ShouldEnableUserAndClearVerificationData() {
        VerifyRequest verifyRequest = createValidVerifyRequest();
        User user = User.builder()
                .email(verifyRequest.getEmail())
                .verificationCode(verifyRequest.getVerificationCode())
                .verificationExpiration(LocalDateTime.now().plusMinutes(10))
                .enabled(false)
                .build();

        when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));

        authService.verifyUser(verifyRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertTrue(savedUser.isEnabled());
        assertNull(savedUser.getVerificationCode());
        assertNull(savedUser.getVerificationExpiration());
    }

    // --- Valid I/O (Validation) Tests ---

    // Email Validation (3 Tests)
    @Test
    void validation_VerifyEmailNull_ShouldFail() {
        VerifyRequest request = createValidVerifyRequest();
        request.setEmail(null);
        Set<ConstraintViolation<VerifyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_VerifyEmailEmpty_ShouldFail() {
        VerifyRequest request = createValidVerifyRequest();
        request.setEmail("");
        Set<ConstraintViolation<VerifyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_VerifyEmailInvalidFormat_ShouldFail() {
        VerifyRequest request = createValidVerifyRequest();
        request.setEmail("invalid-email-format");
        Set<ConstraintViolation<VerifyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // Verification Code Validation (3 Tests)
    @Test
    void validation_VerificationCodeTooLow_ShouldFail() {
        VerifyRequest request = createValidVerifyRequest();
        request.setVerificationCode("099999"); // Under 100000 boundary
        Set<ConstraintViolation<VerifyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_VerificationCodeTooHigh_ShouldFail() {
        VerifyRequest request = createValidVerifyRequest();
        request.setVerificationCode("1000000"); // Above 999999 boundary
        Set<ConstraintViolation<VerifyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_VerificationCodeContainsNonNumeric_ShouldFail() {
        VerifyRequest request = createValidVerifyRequest();
        request.setVerificationCode("123a56"); // Contains non-digits
        Set<ConstraintViolation<VerifyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // --- Business Logic Edge Cases ---

    @Test
    void verifyUser_EmailNotFound_ShouldThrowException() {
        VerifyRequest verifyRequest = createValidVerifyRequest();
        when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.verifyUser(verifyRequest));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyUser_AlreadyEnabled_ShouldThrowException() {
        VerifyRequest verifyRequest = createValidVerifyRequest();
        User user = User.builder()
                .email(verifyRequest.getEmail())
                .enabled(true)
                .build();

        when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.verifyUser(verifyRequest));

        assertEquals("Account is already verified", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyUser_CodeExpired_ShouldThrowException() {
        VerifyRequest verifyRequest = createValidVerifyRequest();
        User user = User.builder()
                .email(verifyRequest.getEmail())
                .verificationCode(verifyRequest.getVerificationCode())
                .verificationExpiration(LocalDateTime.now().minusMinutes(1)) // Expired 1 minute ago
                .enabled(false)
                .build();

        when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.verifyUser(verifyRequest));

        assertEquals("Verification code expired", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyUser_InvalidCode_ShouldThrowException() {
        VerifyRequest verifyRequest = createValidVerifyRequest();
        User user = User.builder()
                .email(verifyRequest.getEmail())
                .verificationCode("654321") // Doesn't match request code "123456"
                .verificationExpiration(LocalDateTime.now().plusMinutes(10))
                .enabled(false)
                .build();

        when(userRepository.findByEmail(verifyRequest.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.verifyUser(verifyRequest));

        assertEquals("Invalid verification code", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // ADDITIONAL HELPERS
    // ==========================================
    private VerifyRequest createValidVerifyRequest() {
        VerifyRequest request = new VerifyRequest();
        request.setEmail("valid@example.com");
        request.setVerificationCode("123456");
        return request;
    }


}
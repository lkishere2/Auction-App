package com.auction.app.users;

import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserController;
import com.auction.app.domains.users.users.UserService;
import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import com.auction.app.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        User currentUser = User.builder()
                .id(1L)
                .username("buyer")
                .email("buyer@example.com")
                .password("password")
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // METHOD 1: getCurrentUser (3 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void getCurrentUser_WhenAuthenticated_ShouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buyer@example.com"))
                .andExpect(jsonPath("$.displayName").value("buyer"));
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void getCurrentUser_WhenAuthenticationIsMissing_ShouldThrowException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/me")))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void getCurrentUser_WhenPrincipalIsNotUser_ShouldThrowException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-user", null)
        );

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/me")))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasRootCauseInstanceOf(ClassCastException.class);
    }

    // =========================================================================
    // METHOD 2: getCurrentUserInformation (2 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void getCurrentUserInformation_WhenCalled_ShouldReturnUserInfo() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(UserResponse.builder()
                .username("buyer")
                .email("buyer@example.com")
                .balance(new BigDecimal("150.00"))
                .build());

        mockMvc.perform(get("/api/v1/users/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("buyer"))
                .andExpect(jsonPath("$.email").value("buyer@example.com"))
                .andExpect(jsonPath("$.balance").value(150.00));

        verify(userService).getCurrentUserInfo();
    }

    // --- Edge Case (1 Test) ---

    @Test
    void getCurrentUserInformation_WhenServiceThrows_ShouldThrowException() {
        when(userService.getCurrentUserInfo()).thenThrow(new RuntimeException("Unexpected user error"));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/info")))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class);
    }

    // =========================================================================
    // METHOD 3: updateUsername (3 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void updateUsername_WhenRequestIsValid_ShouldReturnNoContent() throws Exception {
        UsernameRequest request = createUsernameRequest("newBuyer");

        mockMvc.perform(patch("/api/v1/users/update-username")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateUsername(any(UsernameRequest.class));
    }

    @Test
    void updateUsername_WhenUsernameIsBlank_CurrentControllerStillPassesRequestToService() throws Exception {
        UsernameRequest request = createUsernameRequest("");

        mockMvc.perform(patch("/api/v1/users/update-username")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateUsername(any(UsernameRequest.class));
    }

    // --- Edge Case (1 Test) ---

    @Test
    void updateUsername_WhenBodyIsMissing_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/users/update-username")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // =========================================================================
    // METHOD 4: updateEmail (4 Tests)
    // =========================================================================

    // Happy test

    @Test
    void updateEmail_WhenRequestIsValid_ShouldReturnNoContent() throws Exception {
        EmailRequest request = createEmailRequest("new@example.com");

        mockMvc.perform(patch("/api/v1/users/update-email")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateEmail(any(EmailRequest.class));
    }

    @Test
    void updateEmail_WhenEmailFormatIsInvalid_ReturnsBadRequest() throws Exception {
        // Given
        EmailRequest request = createEmailRequest("not-an-email");

        // When
        mockMvc.perform(patch("/api/v1/users/update-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateEmail(any(EmailRequest.class));
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void updateEmail_WhenBodyIsMissing_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/users/update-email")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void updateEmail_WhenServiceThrows_ShouldThrowException() {
        EmailRequest request = createEmailRequest("buyer@example.com");
        org.mockito.Mockito.doThrow(new RuntimeException("New email must be different from current email"))
                .when(userService).updateEmail(any(EmailRequest.class));

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/v1/users/update-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class);
    }

    // =========================================================================
    // METHOD 5: updatePassword (6 Tests)
    // =========================================================================

    // --- Happy Paths (3 Tests) ---

    @Test
    void updatePassword_WhenRequestIsValid_ShouldReturnNoContent() throws Exception {
        PasswordRequest request = createPasswordRequest("oldPassword", "newPassword");

        mockMvc.perform(patch("/api/v1/users/update-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updatePassword(any(PasswordRequest.class));
    }

    @Test
    void updatePassword_WhenNewPasswordLengthIsMinBoundary_ShouldReturnNoContent() throws Exception {
        PasswordRequest request = createPasswordRequest("oldPassword", "123456");

        mockMvc.perform(patch("/api/v1/users/update-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updatePassword(any(PasswordRequest.class));
    }

    @Test
    void updatePassword_WhenNewPasswordLengthIsMaxBoundary_ShouldReturnNoContent() throws Exception {
        PasswordRequest request = createPasswordRequest("oldPassword", "123456789012345");

        mockMvc.perform(patch("/api/v1/users/update-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updatePassword(any(PasswordRequest.class));
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void updatePassword_WhenCurrentPasswordIsBlank_ShouldReturnBadRequest() throws Exception {
        PasswordRequest request = createPasswordRequest("", "newPassword");

        mockMvc.perform(patch("/api/v1/users/update-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void updatePassword_WhenNewPasswordTooShort_ShouldReturnBadRequest() throws Exception {
        PasswordRequest request = createPasswordRequest("oldPassword", "12345");

        mockMvc.perform(patch("/api/v1/users/update-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void updatePassword_WhenNewPasswordTooLong_ShouldReturnBadRequest() throws Exception {
        PasswordRequest request = createPasswordRequest("oldPassword", "1234567890123456");

        mockMvc.perform(patch("/api/v1/users/update-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // =========================================================================
    // METHOD 6: getAllUsers (5 Tests)
    // =========================================================================

    // --- Happy Paths (3 Tests) ---

    @Test
    void getAllUsers_WhenCalled_ShouldReturnUserPage() throws Exception {
        Page<UserResponse> response = new PageImpl<>(List.of(createUserResponse("buyer", "buyer@example.com")));
        when(userService.getAllUsers(0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/admin/all")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("buyer"))
                .andExpect(jsonPath("$.content[0].email").value("buyer@example.com"));

        verify(userService).getAllUsers(0, 10);
    }

    @Test
    void getAllUsers_WhenParamsAreMissing_ShouldUseDefaultPagination() throws Exception {
        Page<UserResponse> response = new PageImpl<>(List.of());
        when(userService.getAllUsers(0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(userService).getAllUsers(0, 10);
    }

    @Test
    void getAllUsers_WhenBoundarySizeOne_ShouldPassParamsToService() throws Exception {
        Page<UserResponse> response = new PageImpl<>(List.of(createUserResponse("buyer", "buyer@example.com")));
        when(userService.getAllUsers(0, 1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/admin/all")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("buyer@example.com"));

        verify(userService).getAllUsers(0, 1);
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void getAllUsers_WhenPageIsNegative_CurrentControllerStillPassesParamsToService() throws Exception {
        Page<UserResponse> response = new PageImpl<>(List.of());
        when(userService.getAllUsers(-1, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/admin/all")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(userService).getAllUsers(-1, 10);
    }

    @Test
    void getAllUsers_WhenSizeIsZero_CurrentControllerStillPassesParamsToService() throws Exception {
        Page<UserResponse> response = new PageImpl<>(List.of());
        when(userService.getAllUsers(0, 0)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/admin/all")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk());

        verify(userService).getAllUsers(0, 0);
    }

    // =========================================================================
    // METHOD 7: disableUser (3 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void disableUser_WhenIdExists_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/users/admin/disable/{id}", 2L))
                .andExpect(status().isNoContent());

        verify(userService).disableUser(2L);
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void disableUser_WhenIdIsZero_CurrentControllerStillPassesIdToService() throws Exception {
        mockMvc.perform(patch("/api/v1/users/admin/disable/{id}", 0L))
                .andExpect(status().isNoContent());

        verify(userService).disableUser(0L);
    }

    @Test
    void disableUser_WhenIdIsNotNumber_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/users/admin/disable/not-a-number"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    private UsernameRequest createUsernameRequest(String username) {
        UsernameRequest request = new UsernameRequest();
        request.setUsername(username);
        return request;
    }

    private EmailRequest createEmailRequest(String email) {
        EmailRequest request = new EmailRequest();
        request.setEmail(email);
        return request;
    }

    private PasswordRequest createPasswordRequest(String currentPassword, String newPassword) {
        PasswordRequest request = new PasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private UserResponse createUserResponse(String username, String email) {
        return UserResponse.builder()
                .username(username)
                .email(email)
                .balance(new BigDecimal("150.00"))
                .build();
    }
}

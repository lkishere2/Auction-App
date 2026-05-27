package com.auction.app.controllers;

import com.auction.app.MainController;
import com.auction.app.domains.auth.auth.AuthController;
import com.auction.app.domains.auth.auth.dtos.AuthResponse;
import com.auction.app.domains.auth.auth.dtos.LoginRequest;
import com.auction.app.infrastructure.security.CachedUserDetails;
import com.auction.app.infrastructure.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;

@Component
public class LoginBoxController {

    @Autowired
    private AuthController authController;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MainController mainController;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showErrorAlert("Validation Error", "Please provide both fields.");
            return;
        }

        try {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword(password);

            // 1. Create a runtime proxy stub to cleanly substitute real HTTP request items
            HttpServletRequest mockRequest = createMockHttpServletRequest();

            // 2. Call your producers passing the custom proxy instead of null
            ResponseEntity<AuthResponse> responseEntity = authController.login(loginRequest, mockRequest);
            AuthResponse response = responseEntity.getBody();

            if (response != null && response.getAccessToken() != null) {
                // 3. Load user account payload
                CachedUserDetails userDetails = (CachedUserDetails) userDetailsService.loadUserByUsername(email);

                // 4. Securely store credentials inside the application state session block
                userSession.setAccessToken(response.getAccessToken());
                userSession.setRefreshToken(response.getRefreshToken());
                userSession.setUserDetails(userDetails);

                // 5. Establish local thread validation
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("Login Success! Secure session token context established inside the JVM thread container.");
                showSuccessAlert("Welcome!", "Login successful! Stored credentials securely.");

                // When your dashboard layouts are built, uncomment to transition out of login view:
                // mainController.navigateTo("/ui/DashboardView.fxml");

            } else {
                showErrorAlert("Sign In Failed", "Authentication returned an invalid credential confirmation schema.");
            }
        } catch (Exception ex) {
            showErrorAlert("Authentication Error", ex.getMessage());
        }
    }

    @FXML
    private void navigateToRegister(ActionEvent event) {
        mainController.navigateTo("/ui/RegisterView.fxml");
    }

    /**
     * Builds a lightweight runtime proxy intercepting calls to User-Agent headers
     * and Remote IP Addresses to stop server-side audit trails from throwing a NullPointerException.
     */
    private HttpServletRequest createMockHttpServletRequest() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName()) && args.length > 0 && "User-Agent".equals(args[0])) {
                        return "JavaFX Desktop Application Client";
                    }
                    if ("getRemoteAddr".equals(method.getName())) {
                        return "127.0.0.1";
                    }
                    return null;
                }
        );
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
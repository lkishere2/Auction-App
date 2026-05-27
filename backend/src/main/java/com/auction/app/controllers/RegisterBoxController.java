package com.auction.app.controllers;

import com.auction.app.MainController;
import com.auction.app.domains.auth.auth.AuthController;
import com.auction.app.domains.auth.auth.dtos.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;

@Component
public class RegisterBoxController {

    @Autowired
    private AuthController authController; // Lazily reusing the backend web controller bean natively

    @Autowired
    private MainController mainController; // Central layout window navigation router context

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showErrorAlert("Validation Error", "Please fill in all registration parameters.");
            return;
        }

        try {
            // 1. Build backend data payload
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername(username);
            registerRequest.setEmail(email);
            registerRequest.setPassword(password);

            // 2. Generate a request proxy stub so the backend mail system safely gets device metadata
            HttpServletRequest mockRequest = createMockHttpServletRequest();

            // 3. Call your existing REST endpoint directly inside our JVM instance memory space
            ResponseEntity<String> response = authController.register(registerRequest, mockRequest);

            showSuccessAlert("Account Registered!", response.getBody() + "\nPlease check your mailbox.");

            // 4. Redirect straight to the upcoming dynamic code verification screen workflow
            mainController.navigateTo("/ui/VerificationView.fxml");

        } catch (Exception ex) {
            showErrorAlert("Registration Failure", ex.getMessage());
        }
    }

    @FXML
    private void navigateToLogin(ActionEvent event) {
        // Direct route transition back to the login page view layout container context
        mainController.navigateTo("/ui/LoginView.fxml");
    }

    @FXML
    private void navigateToForgotPassword(ActionEvent event) {
        mainController.navigateTo("/ui/ForgotPasswordView.fxml");
    }

    /**
     * Builds a reflective runtime proxy stub for HttpServletRequest to bypass
     * User-Agent and Remote Address audit trail checks safely inside the background thread.
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
package com.auction.app.controllers.auth;

import com.auction.app.MainController;
import com.auction.app.domains.auth.auth.AuthController;
import com.auction.app.domains.auth.auth.dtos.EmailRequest;
import com.auction.app.domains.auth.auth.dtos.ResetPasswordRequest;
import com.auction.app.domains.auth.auth.dtos.VerifyRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ForgetPasswordBoxController {

    private enum ResetState { REQUEST, VERIFY, CONFIRM }
    private ResetState currentState = ResetState.REQUEST;

    @Autowired
    private AuthController authController; // Reusing your production REST Controller methods directly

    @Autowired
    private MainController mainController; // Standard window route engine configuration tool

    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;

    @FXML private VBox emailFormBlock;
    @FXML private VBox codeFormBlock;
    @FXML private VBox passwordFormBlock;

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField passwordField;
    @FXML private Button actionButton;

    @FXML
    private void handleAction(ActionEvent event) {
        switch (currentState) {
            case REQUEST -> processCodeRequest();
            case VERIFY -> processCodeVerification();
            case CONFIRM -> processPasswordConfirmation();
        }
    }

    private void processCodeRequest() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showErrorAlert("Input Validation", "Please enter your registered email address.");
            return;
        }

        try {
            EmailRequest request = new EmailRequest();
            request.setEmail(email);

            ResponseEntity<String> response = authController.requestPasswordReset(request);
            showSuccessAlert("Code Sent", response.getBody());

            // Advance view state to verification pane layout settings
            currentState = ResetState.VERIFY;
            emailField.setEditable(false); // Lock the email text row field input
            codeFormBlock.setManaged(true);
            codeFormBlock.setVisible(true);

            descriptionLabel.setText("Enter the reset authorization code sent to your email.");
            actionButton.setText("Verify Reset Code");

        } catch (Exception ex) {
            showErrorAlert("Request Refused", ex.getMessage());
        }
    }

    private void processCodeVerification() {
        String email = emailField.getText().trim();
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showErrorAlert("Input Validation", "Please fill out the reset token code input row slot.");
            return;
        }

        try {
            VerifyRequest request = new VerifyRequest();
            request.setEmail(email);
            request.setVerificationCode(code);

            ResponseEntity<String> response = authController.verifyPasswordReset(request);
            showSuccessAlert("Code Verified!", response.getBody());

            // Transition state directly to the finalize confirmation panels
            currentState = ResetState.CONFIRM;
            codeFormBlock.setManaged(false);
            codeFormBlock.setVisible(false);
            passwordFormBlock.setManaged(true);
            passwordFormBlock.setVisible(true);

            descriptionLabel.setText("Configure a brand new login security password account profile asset.");
            actionButton.setText("Confirm New Password");

        } catch (Exception ex) {
            showErrorAlert("Verification Denied", ex.getMessage());
        }
    }

    private void processPasswordConfirmation() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (password.length() < 6) {
            showErrorAlert("Input Validation", "Passwords must contain a minimum threshold length boundary value of 6 items.");
            return;
        }

        try {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setEmail(email);
            request.setPassword(password);

            ResponseEntity<String> response = authController.resetPassword(request);
            showSuccessAlert("Profile Updated Successfully!", response.getBody() + "\nYou can now log in safely.");

            // Loop workflow entirely back to original Sign In viewport mapping layout
            mainController.navigateTo("/ui/views/auth/LoginView.fxml");

        } catch (Exception ex) {
            showErrorAlert("Update Dropped", ex.getMessage());
        }
    }

    @FXML
    private void navigateToLogin(ActionEvent event) {
        mainController.navigateTo("/ui/views/auth/LoginView.fxml");
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
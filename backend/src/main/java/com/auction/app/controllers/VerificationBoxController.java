package com.auction.app.controllers;

import com.auction.app.MainController;
import com.auction.app.domains.auth.auth.AuthController;
import com.auction.app.domains.auth.auth.dtos.EmailRequest;
import com.auction.app.domains.auth.auth.dtos.VerifyRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class VerificationBoxController {

    @Autowired
    private AuthController authController; // Reusing your production REST Controller methods inside the JVM

    @Autowired
    private MainController mainController; // Primary view window shell navigation routing tool

    @FXML
    private TextField emailField;

    @FXML
    private TextField codeField;

    @FXML
    private Button verifyButton;

    @FXML
    private void handleVerify(ActionEvent event) {
        String email = emailField.getText().trim();
        String code = codeField.getText().trim();

        if (email.isEmpty() || code.isEmpty()) {
            showErrorAlert("Validation Error", "Please provide both your registered email and the 6-digit code.");
            return;
        }

        try {
            // 1. Map parameters to the expected VerifyRequest data payload
            VerifyRequest verifyRequest = new VerifyRequest();
            verifyRequest.setEmail(email);
            verifyRequest.setVerificationCode(code);

            // 2. Pass data directly through the AuthController endpoint bean natively
            ResponseEntity<String> response = authController.verifyUser(verifyRequest);

            showSuccessAlert("Account Activated!", response.getBody() + "\nYou can now sign in safely.");

            // 3. Verification complete! Redirect the user straight back to the login screen
            mainController.navigateTo("/ui/LoginView.fxml");

        } catch (Exception ex) {
            showErrorAlert("Verification Failed", ex.getMessage());
        }
    }

    @FXML
    private void handleResendCode(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showErrorAlert("Input Needed", "Please fill in the email input box field to request a token reset.");
            return;
        }

        try {
            // 1. Build standard EmailRequest
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setEmail(email);

            // 2. Call the resend backend lifecycle method
            ResponseEntity<String> response = authController.resendVerificationCode(emailRequest);

            showSuccessAlert("Token Dispatched", response.getBody());

        } catch (Exception ex) {
            showErrorAlert("Email Transmission Error", ex.getMessage());
        }
    }

    @FXML
    private void navigateToLogin(ActionEvent event) {
        mainController.navigateTo("/ui/LoginView.fxml");
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
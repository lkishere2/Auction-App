package com.auction.app.controllers.auth;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class VerificationViewController {

    @FXML
    private VerificationBoxController verificationBoxController;

    @FXML
    public void initialize() {
        System.out.println("Parent VerificationViewController initialized successfully.");
    }
}
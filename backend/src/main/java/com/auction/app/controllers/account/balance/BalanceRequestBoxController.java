package com.auction.app.controllers.account.balance;

import com.auction.app.domains.transaction.TransactionController;
import com.auction.app.domains.transaction.dtos.TransactionRequest;
import com.auction.app.domains.transaction.model.TransactionType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BalanceRequestBoxController {

    @FXML private Label modalTitleLabel;
    @FXML private Label modalDescriptionLabel;
    @FXML private TextField amountInputField;
    @FXML private Label errorMessageLabel;
    @FXML private Button actionSubmitButton;

    @Autowired private TransactionController transactionController;

    private TransactionType currentContextType;
    private BalanceViewController parentContext;

    public void configureModalContext(TransactionType type, BalanceViewController parent) {
        this.currentContextType = type;
        this.parentContext = parent;

        if (type == TransactionType.DEPOSIT) {
            modalTitleLabel.setText("Request Deposit");
            modalDescriptionLabel.setText("Submit a request to deposit funds. An administrator will review and verify your deposit confirmation.");
            actionSubmitButton.setText("Submit Deposit");
            actionSubmitButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            modalTitleLabel.setText("Request Withdrawal");
            modalDescriptionLabel.setText("Submit a request to withdraw funds. Your request will process as pending until approved by an administrator.");
            actionSubmitButton.setText("Submit Withdrawal");
            actionSubmitButton.setStyle("-fx-background-color: #F43F5E; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleSubmitTransaction() {
        String inputString = amountInputField.getText().trim();
        errorMessageLabel.setVisible(false);

        if (inputString.isEmpty()) {
            displayError("Amount field cannot be left empty.");
            return;
        }

        BigDecimal requestedAmount;
        try {
            requestedAmount = new BigDecimal(inputString);
            if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                displayError("Please provide a valid, positive transaction amount.");
                return;
            }
        } catch (NumberFormatException ex) {
            displayError("Invalid numeric format. Ensure amount uses standard digit notation.");
            return;
        }

        // Initialize input values into your backend TransactionRequest structure
        TransactionRequest requestPayload = new TransactionRequest();
        requestPayload.setAmount(requestedAmount);
        requestPayload.setType(currentContextType);

        new Thread(() -> {
            try {
                ResponseEntity<?> response = transactionController.createTransaction(requestPayload);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Platform.runLater(() -> {
                        parentContext.refreshWalletWorkspace();
                        handleCloseModal();
                    });
                } else {
                    Platform.runLater(() -> displayError("Server rejected request. Verify operational bounds."));
                }
            } catch (Exception e) {
                Platform.runLater(() -> displayError("Connection failure: Could not reach transaction service."));
            }
        }).start();
    }

    @FXML
    private void handleCloseModal() {
        if (parentContext == null) return;
        StackPane overlay = parentContext.getModalOverlayTarget();
        overlay.getChildren().clear();
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setMouseTransparent(true);
    }

    private void displayError(String text) {
        errorMessageLabel.setText(text);
        errorMessageLabel.setVisible(true);
    }
}
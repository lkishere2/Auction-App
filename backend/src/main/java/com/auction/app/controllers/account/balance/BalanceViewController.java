package com.auction.app.controllers.account.balance;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class BalanceViewController {

    @FXML private StackPane modalOverlayTarget;

    // ✅ FIXED: Removed Lombok @Getter to prevent interference with JavaFX FXML field injection mappings
    @FXML private UserBalanceCardController userBalanceCardController;
    @FXML private TransactionHistoryBoxController transactionHistoryBoxController;

    @FXML
    public void initialize() {
        System.out.println("Initializing main Balance View Layout controller dashboard container...");

        // Link parent contextual frameworks directly into our sub-controller instances
        if (userBalanceCardController != null) {
            userBalanceCardController.setParentController(this);
        } else {
            System.err.println("WARNING: userBalanceCardController failed to inject into BalanceViewController!");
        }

        if (transactionHistoryBoxController != null) {
            transactionHistoryBoxController.setParentController(this);
        } else {
            System.err.println("WARNING: transactionHistoryBoxController failed to inject into BalanceViewController!");
        }
    }

    public StackPane getModalOverlayTarget() {
        return this.modalOverlayTarget;
    }

    // ✅ FIXED: Explicit native getters instead of Lombok annotations ensure total compatibility
    public UserBalanceCardController getUserBalanceCardController() {
        return this.userBalanceCardController;
    }

    public TransactionHistoryBoxController getTransactionHistoryBoxController() {
        return this.transactionHistoryBoxController;
    }

    /**
     * Invoked by modals to refresh sibling states dynamically
     * when background database mutations complete successfully.
     */
    public void refreshWalletWorkspace() {
        System.out.println("Refreshing database states across all balance view workspace containers...");
        if (userBalanceCardController != null) {
            userBalanceCardController.loadActiveUserMetrics();
        }
        if (transactionHistoryBoxController != null) {
            transactionHistoryBoxController.loadHistoricalPageData();
        }
    }
}
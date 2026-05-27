package com.auction.app.controllers.account;

import com.auction.app.MainController;
import com.auction.app.controllers.NavbarItemController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AccountNavbarController {

    @Autowired
    private MainController mainController;

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private AccountViewController accountViewController;

    // 1. Inject the visual UI Root Nodes matching the FXML fx:id fields
    @FXML private Parent navProfile;
    @FXML private Parent navBalance;
    @FXML private Parent navInventory;
    @FXML private Parent navSettings;
    @FXML private Parent navBack;

    // 2. Inject sub-controllers (JavaFX appends "Controller" directly to the fx:id strings)
    @FXML private NavbarItemController navProfileController;
    @FXML private NavbarItemController navBalanceController;
    @FXML private NavbarItemController navInventoryController;
    @FXML private NavbarItemController navSettingsController;
    @FXML private NavbarItemController navBackController;

    @FXML
    public void initialize() {
        // Configure textual values dynamically utilizing your precise component methods
        if (navProfileController != null) navProfileController.setItemText("Profile");
        if (navBalanceController != null) navBalanceController.setItemText("Balance");
        if (navInventoryController != null) navInventoryController.setItemText("Inventory");
        if (navSettingsController != null) navSettingsController.setItemText("Settings");
        if (navBackController != null) navBackController.setItemText("Back to Dashboard");

        // Attach Mouse Click Event Listeners securely to the raw visual containers
        if (navProfile != null) navProfile.setOnMouseClicked(e -> handlePanelSwap("Profile"));
        if (navBalance != null) navBalance.setOnMouseClicked(e -> handlePanelSwap("Balance"));
        if (navInventory != null) navInventory.setOnMouseClicked(e -> handlePanelSwap("Inventory"));
        if (navSettings != null) navSettings.setOnMouseClicked(e -> handlePanelSwap("Settings"));

        if (navBack != null) {
            navBack.setOnMouseClicked(e -> {
                System.out.println("Returning to main dashboard framework...");
                mainController.navigateTo("/ui/views/home/HomeView.fxml");
            });
        }
    }

    private void handlePanelSwap(String panelName) {
        System.out.println("Switching Account View focus to: " + panelName);

        String fxmlPath = null;

        // Route matching criteria based on user sidebar selections
        if ("Settings".equals(panelName)) {
            fxmlPath = "/ui/views/account/settings/SettingView.fxml";
        } else if ("Balance".equals(panelName)) {
            fxmlPath = "/ui/views/account/balance/BalanceView.fxml";
        }

        // If an explicit route mapping matches, process dynamic FXML scene injection
        if (fxmlPath != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(springContext::getBean);

                Parent viewNode = loader.load();

                if (accountViewController != null && accountViewController.getAccountContentCanvas() != null) {
                    accountViewController.getAccountContentCanvas().getChildren().setAll(viewNode);
                } else {
                    System.err.println("Target account content canvas panel frame wrapper container is unavailable.");
                }
            } catch (Exception e) {
                System.err.println("Failed to cleanly load dynamic view pane [" + panelName + "]: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
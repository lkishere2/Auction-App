package com.auction.app.controllers.account.settings;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class SettingViewController {

    // Inject nested controllers using standard sub-component naming rules
    @FXML private UserProfileCardController userProfileCardController;
    @FXML private UpdateProfileBoxController updateProfileBoxController;

    @FXML
    public void initialize() {
        System.out.println("Assembling Settings Panel Form Views...");

        // Link component interactions using a runnable callback loop
        if (updateProfileBoxController != null && userProfileCardController != null) {
            updateProfileBoxController.setOnUpdateSuccess(() -> {
                System.out.println("Profile updated! Refreshing header visuals...");
                userProfileCardController.refreshUserData();
            });
        }
    }
}
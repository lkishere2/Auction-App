package com.auction.app.controllers.account.profile;

import com.auction.app.infrastructure.security.SecurityUtils;
import com.auction.app.domains.users.users.model.User;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileViewController {

    private final SecurityUtils securityUtils;

    // Inject nested include components automatically mapped via JavaFX conventions
    @FXML private HBox userStatsBox;
    @FXML private UserStatsBoxController userStatsBoxController;

    @FXML private VBox auctionGrid;
    @FXML private AuctionGridController auctionGridController;

    @FXML
    public void initialize() {
        try {
            User activeUser = securityUtils.getCurrentUser();

            // Sync user data to components
            if (userStatsBoxController != null) {
                userStatsBoxController.renderStatsHeader(activeUser);
            }
            if (auctionGridController != null) {
                auctionGridController.loadUserGridFeeds();
            }
        } catch (Exception e) {
            System.err.println("Failed to bind context session definitions into profile sheets: " + e.getMessage());
        }
    }
}
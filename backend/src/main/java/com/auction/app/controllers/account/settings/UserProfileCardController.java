package com.auction.app.controllers.account.settings;

import com.auction.app.domains.users.users.UserController;
import com.auction.app.domains.users.users.dtos.UserResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class UserProfileCardController {

    @FXML private ImageView avatarImageView;
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;

    @Autowired
    private UserController userController;

    @FXML
    public void initialize() {
        // Build a perfect circular clipping mask around the Avatar Viewport bounds
        Circle clip = new Circle(40, 40, 40);
        avatarImageView.setClip(clip);

        refreshUserData();
    }

    /**
     * Hits your live UserController endpoint to get a fresh UserResponse DTO snapshot
     * directly from your database, preventing stale cache rendering bugs!
     */
    public void refreshUserData() {
        try {
            ResponseEntity<UserResponse> response = userController.getCurrentUserInformation();
            if (response != null && response.getBody() != null) {
                UserResponse userInfo = response.getBody();

                // Populate UI from the live UserResponse fields!
                usernameLabel.setText(userInfo.getUsername());

                // Balance monitoring helper preview label
                roleLabel.setText(userInfo.getBalance() != null ? "Balance: $" + userInfo.getBalance() : "$0.00");

                String avatarUrl = userInfo.getProfileImageUrl(); // Utilizing your exact .getProfileImageUrl() DTO method

                if (avatarUrl == null || avatarUrl.isBlank()) {
                    avatarUrl = "https://res.cloudinary.com/dbjdacw6w/image/upload/v1/placeholders/default-avatar.png";
                }

                String finalAvatarUrl = avatarUrl;
                // Safely update the image view off the worker thread if called during background tasks
                Platform.runLater(() -> {
                    try {
                        Image image = new Image(finalAvatarUrl, true);
                        avatarImageView.setImage(image);
                    } catch (Exception e) {
                        System.out.println("Skipped avatar asset stream: " + e.getMessage());
                    }
                });
            }
        } catch (Exception ex) {
            System.err.println("Failed to pull live user response info context profile: " + ex.getMessage());
        }
    }
}
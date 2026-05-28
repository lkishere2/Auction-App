package com.auction.app.controllers.account.profile;

import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.auction.auction.AuctionRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStatsBoxController {

    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;

    @FXML private ImageView avatarImageView;
    @FXML private Label usernameLabel;
    @FXML private Label displayNameLabel;
    @FXML private Label auctionCountLabel;
    @FXML private Label itemsCountLabel;

    @FXML
    public void initialize() {
        // Apply a circular clip to match Instagram's avatar framing look
        Circle clip = new Circle(45, 45, 45);
        avatarImageView.setClip(clip);
    }

    public void renderStatsHeader(User user) {
        usernameLabel.setText("@" + user.getUsername());
        displayNameLabel.setText(user.getDisplayName());

        // Read total records directly from database count states
        long totalProducts = productRepository.findAllUserProducts(user.getId(), Pageable.unpaged()).getTotalElements();
        long totalAuctions = auctionRepository.findIdsBySellerIdOrderByStartTime(user.getId(), Pageable.unpaged()).getTotalElements();

        auctionCountLabel.setText(String.valueOf(totalAuctions));
        itemsCountLabel.setText(String.valueOf(totalProducts));

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            try {
                avatarImageView.setImage(new Image(user.getProfileImageUrl(), true));
            } catch (Exception e) {
                loadPlaceholderAvatar();
            }
        } else {
            loadPlaceholderAvatar();
        }
    }

    private void loadPlaceholderAvatar() {
        avatarImageView.setImage(new Image(getClass().getResourceAsStream("/ui/images/placeholder.png")));
    }
}
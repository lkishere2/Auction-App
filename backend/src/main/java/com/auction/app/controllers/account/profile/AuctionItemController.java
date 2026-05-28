package com.auction.app.controllers.account.profile;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuctionItemController {

    @FXML private StackPane rootPane;
    @FXML private ImageView postImageView;
    @FXML private VBox hoverOverlay;
    @FXML private Label overlayTitleLabel;
    @FXML private Label overlayPriceLabel;
    @FXML private Label overlayStatusLabel;

    @FXML
    public void initialize() {
        // Set up the Instagram look: trigger text overlay cards on hover
        rootPane.setOnMouseEntered(e -> hoverOverlay.setVisible(true));
        rootPane.setOnMouseExited(e -> hoverOverlay.setVisible(false));
    }

    public void setAuctionPostData(AuctionResponse auction) {
        overlayTitleLabel.setText(auction.getProductName());
        overlayPriceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));
        overlayStatusLabel.setText(auction.getStatus().name());

        if (auction.getProductImageUrl() != null && !auction.getProductImageUrl().isBlank()) {
            try {
                postImageView.setImage(new Image(auction.getProductImageUrl(), true));
            } catch (Exception e) {
                loadPlaceholderPost();
            }
        } else {
            loadPlaceholderPost();
        }
    }

    private void loadPlaceholderPost() {
        postImageView.setImage(new Image(getClass().getResourceAsStream("/ui/images/placeholder.png")));
    }
}
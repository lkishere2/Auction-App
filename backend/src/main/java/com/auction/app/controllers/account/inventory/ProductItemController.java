package com.auction.app.controllers.account.inventory;

import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.domains.products.model.Tag;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProductItemController {

    @FXML private ImageView productImageView;
    @FXML private Label productNameLabel;
    @FXML private Label quantityLabel;
    @FXML private HBox tagsContainer;

    private ProductResponse currentProduct;
    private Consumer<ProductResponse> onAuctionActionHandler;

    public void setProductData(ProductResponse product, Consumer<ProductResponse> onAuctionActionHandler) {
        this.currentProduct = product;
        this.onAuctionActionHandler = onAuctionActionHandler;

        productNameLabel.setText(product.getProductName());
        quantityLabel.setText(String.valueOf(product.getQuantity()));

        if (product.getProductImageUrl() != null && !product.getProductImageUrl().isBlank()) {
            try {
                productImageView.setImage(new Image(product.getProductImageUrl(), true));
            } catch (Exception e) {
                loadPlaceholderImage();
            }
        } else {
            loadPlaceholderImage();
        }

        tagsContainer.getChildren().clear();
        if (product.getTags() != null) {
            for (Tag tag : product.getTags()) {
                Label tagBadge = new Label(tag.name());
                tagBadge.setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4;");
                tagsContainer.getChildren().add(tagBadge);
            }
        }
    }

    @FXML
    public void handleCreateAuctionClick(javafx.event.ActionEvent event) {
        // Prevent the click from bubbling up to the card's main area (which opens the edit modal)
        event.consume();
        if (onAuctionActionHandler != null && currentProduct != null) {
            onAuctionActionHandler.accept(currentProduct);
        }
    }

    private void loadPlaceholderImage() {
        productImageView.setImage(new Image(getClass().getResourceAsStream("/ui/images/placeholder.png")));
    }
}
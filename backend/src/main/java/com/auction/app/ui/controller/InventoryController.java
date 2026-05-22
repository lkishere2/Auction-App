package com.auction.app.ui.controller;

import com.auction.app.infrastructure.config.SpringContext;
import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.security.LoggedUser;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import com.auction.app.ui.SceneManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InventoryController implements Initializable {

    @FXML
    private FlowPane productContainer;

    @FXML
    private Button createButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (productContainer != null) {
            productContainer.setPrefWrapLength(900);
            productContainer.setPadding(new Insets(10));
        }

        loadProducts();
    }

    @FXML
    private void onBack() {
        try {
            SceneManager.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        try {
            ProductRepository repo = SpringContext.getBean(ProductRepository.class);
            Long ownerId = LoggedUser.get() != null ? LoggedUser.get().getId() : null;

            if (ownerId == null) return;

            var page = repo.findByKeywordAndTags(ownerId, null, null, org.springframework.data.domain.PageRequest.of(0, 100));
            List<Product> products = page.getContent();

            productContainer.getChildren().clear();
            for (Product p : products) {
                productContainer.getChildren().add(createProductCard(p));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPrefSize(300, 180);
        card.setPadding(new Insets(12));

        Label title = new Label(p.getProductName() + " x" + p.getQuantity());
        title.getStyleClass().add("card-title");

        Label desc = new Label("Description: " + (p.getDescription() != null ? p.getDescription() : "-"));
        desc.setWrapText(true);

        HBox bottom = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button view = new Button("View");
        view.setOnAction(evt -> showProductModal(p));

        bottom.getChildren().addAll(spacer, view);

        card.getChildren().addAll(title, desc, bottom);
        return card;
    }

    private void showProductModal(Product p) {
        if (LoggedUser.get() == null) {
            try { com.auction.app.ui.SceneManager.showAuth(); } catch (Exception e) { e.printStackTrace(); }
            return;
        }
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Product — " + p.getProductName());

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        HBox top = new HBox();
        top.setSpacing(8);
        Button back = new Button("<-");
        back.getStyleClass().add("back-button");
        back.setOnAction(e -> modal.close());
        top.getChildren().add(back);
        root.setTop(top);

        HBox content = new HBox(16);

        ImageView iv = new ImageView();
        iv.setFitWidth(220);
        iv.setFitHeight(220);
        iv.setPreserveRatio(true);
        try {
            Image img = new Image("https://images.unsplash.com/photo-1598550454466-9b7a8f3b1c9d?w=600&q=80", 220, 220, true, true);
            iv.setImage(img);
        } catch (Exception ignored) {}

        VBox right = new VBox(12);
        Label name = new Label(p.getProductName() + " x" + p.getQuantity());
        name.getStyleClass().add("card-title");
        Label description = new Label("Description:\n" + (p.getDescription() != null ? p.getDescription() : "-"));
        description.setWrapText(true);
        Label createdAt = new Label("Creation Date: " + (p.getCreatedAt() != null ? p.getCreatedAt().toString() : "-"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button close = new Button("Close");
        close.setOnAction(e -> modal.close());

        right.getChildren().addAll(name, description, createdAt, spacer, close);

        content.getChildren().addAll(iv, right);
        root.setCenter(content);

        Scene scene = new Scene(root, 700, 300);
        scene.getStylesheets().add(InventoryController.class.getResource("/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.showAndWait();
    }
}

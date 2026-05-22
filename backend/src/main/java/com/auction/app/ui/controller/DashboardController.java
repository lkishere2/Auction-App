package com.auction.app.ui.controller;

import com.auction.app.infrastructure.config.SpringContext;
import com.auction.app.domains.auction.auction.Auction;
import com.auction.app.domains.auction.auction.AuctionRepository;
import com.auction.app.domains.auction.auction.AuctionStatus;
import com.auction.app.security.LoggedUser;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private FlowPane activeContainer;

    @FXML
    private FlowPane endedContainer;

    @FXML
    private FlowPane upcomingContainer;

    @FXML
    private Button signInButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (signInButton != null) {
            if (LoggedUser.get() != null) {
                signInButton.setText("Welcome, " + LoggedUser.get().getDisplayName());
                signInButton.setDisable(true);
            } else {
                signInButton.setText("Sign In");
                signInButton.setDisable(false);
            }
        }

        if (activeContainer != null) {
            activeContainer.setPrefWrapLength(1100);
            activeContainer.setPadding(new Insets(10, 0, 10, 0));
        }
        if (endedContainer != null) {
            endedContainer.setPrefWrapLength(1100);
            endedContainer.setPadding(new Insets(10, 0, 10, 0));
        }
        if (upcomingContainer != null) {
            upcomingContainer.setPrefWrapLength(1100);
            upcomingContainer.setPadding(new Insets(10, 0, 10, 0));
        }

        loadAuctions();
    }

    @FXML
    private void onLogin() {
        try {
            com.auction.app.ui.SceneManager.showAuth();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onInventory() {
        try {
            if (LoggedUser.get() == null) {
                com.auction.app.ui.SceneManager.showAuth();
            } else {
                com.auction.app.ui.SceneManager.showInventory();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAuctions() {
        try {
            AuctionRepository repo = SpringContext.getBean(AuctionRepository.class);

            List<Auction> actives = repo.findByStatusWithDetails(AuctionStatus.ACTIVE);
            List<Auction> endeds = repo.findByStatusWithDetails(AuctionStatus.ENDED);
            List<Auction> upcomings = repo.findByStatusWithDetails(AuctionStatus.UPCOMING);

            if (activeContainer != null) activeContainer.getChildren().clear();
            if (endedContainer != null) endedContainer.getChildren().clear();
            if (upcomingContainer != null) upcomingContainer.getChildren().clear();

            for (Auction a : actives) if (activeContainer != null) activeContainer.getChildren().add(createCard(a));
            for (Auction a : endeds) if (endedContainer != null) endedContainer.getChildren().add(createCard(a));
            for (Auction a : upcomings) if (upcomingContainer != null) upcomingContainer.getChildren().add(createCard(a));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Node createCard(Auction a) {
        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setPrefSize(280, 400);

        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("card-image");
        imagePane.setPrefHeight(180);
        imagePane.setMaxWidth(Double.MAX_VALUE);

        Rectangle backdrop = new Rectangle(280, 180, Color.web("#F3F4F6"));
        backdrop.setArcWidth(36);
        backdrop.setArcHeight(36);

        Label liveBadge = new Label(a.getStatus().name());
        liveBadge.getStyleClass().add("live-badge");
        StackPane.setAlignment(liveBadge, Pos.TOP_LEFT);
        StackPane.setMargin(liveBadge, new Insets(12, 0, 0, 12));

        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        try {
            Image image = new Image("https://images.unsplash.com/photo-1558980664-10b65cd27286?w=900&q=80", 280, 180, false, true, true);
            imageView.setImage(image);
        } catch (Exception ignored) {}

        imagePane.getChildren().addAll(backdrop, imageView, liveBadge);

        VBox content = new VBox(14);
        content.getStyleClass().add("card-stack");
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(a.getProduct() != null ? a.getProduct().getProductName() : "-");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setMaxWidth(240);

        Label bids = new Label((a.getBidCount() != null ? a.getBidCount() : 0) + " bids placed");
        bids.getStyleClass().add("card-meta");

        HBox divider = new HBox();
        divider.getStyleClass().add("card-divider");
        divider.setPadding(new Insets(8, 0, 0, 0));

        HBox summary = new HBox(16);
        summary.getStyleClass().add("card-row");
        summary.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(4);
        Label priceLabel = new Label("Current Bid");
        priceLabel.getStyleClass().add("ends-in-label");
        Label priceValue = new Label(a.getCurrentPrice() != null ? formatCurrency(a.getCurrentPrice().intValue()) : "$");
        priceValue.getStyleClass().add("current-bid-value");
        priceBox.getChildren().addAll(priceLabel, priceValue);

        VBox timeBox = new VBox(4);
        Label endsLabel = new Label("Ends In");
        endsLabel.getStyleClass().add("ends-in-label");
        Label endsValue = new Label(a.getEndTime() != null ? a.getEndTime().toString() : "-");
        endsValue.getStyleClass().add("ends-in-value");
        timeBox.getChildren().addAll(endsLabel, endsValue);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        summary.getChildren().addAll(priceBox, spacer, timeBox);

        Button bidButton = new Button("Place Bid");
        bidButton.getStyleClass().add("place-bid-button");
        bidButton.setMaxWidth(Double.MAX_VALUE);

        if (a.getStatus() == AuctionStatus.ACTIVE) {
            bidButton.setVisible(true);
            bidButton.setDisable(false);
        } else {
            bidButton.setVisible(true);
            bidButton.setDisable(true);
            backdrop.setFill(javafx.scene.paint.Color.web("#E5E7EB"));
            liveBadge.setText(a.getStatus() == AuctionStatus.ENDED ? "Ended" : "Upcoming");
            liveBadge.getStyleClass().add("disabled-badge");
        }

        bidButton.setOnAction(evt -> {
            try {
                if (LoggedUser.get() == null) {
                    com.auction.app.ui.SceneManager.showAuth();
                    return;
                }
                showAuctionModal(a);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        VBox bottom = new VBox(10, divider, summary, bidButton);
        bottom.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(title, bids, bottom);
        VBox.setVgrow(bottom, Priority.ALWAYS);

        card.getChildren().addAll(imagePane, content);
        return card;
    }

    private String formatCurrency(int amount) {
        return "$" + String.format("%,d", amount);
    }

    private void showAuctionModal(Auction a) {
        javafx.stage.Stage modal = new javafx.stage.Stage();
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modal.setTitle("Auction — " + (a.getProduct() != null ? a.getProduct().getProductName() : "-"));

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12);
        root.setPadding(new Insets(12));
        Label title = new Label(a.getProduct() != null ? a.getProduct().getProductName() : "-");
        title.getStyleClass().add("card-title");
        Label status = new Label("Status: " + a.getStatus().name());
        Label current = new Label("Current: " + (a.getCurrentPrice() != null ? formatCurrency(a.getCurrentPrice().intValue()) : "-"));

        Button close = new Button("Close");
        close.setOnAction(e -> modal.close());

        Button place = new Button("Place Bid");
        place.setDisable(a.getStatus() != AuctionStatus.ACTIVE);
        place.setOnAction(e -> {
            // stub: actual bid flow not yet implemented
            modal.close();
        });

        root.getChildren().addAll(title, status, current, place, close);
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 220);
        scene.getStylesheets().add(DashboardController.class.getResource("/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.showAndWait();
    }
}
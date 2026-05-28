package com.auction.app.controllers.account.inventory;

import com.auction.app.domains.products.ProductService;
import com.auction.app.domains.products.dtos.ProductResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InventoryViewController {

    private final ProductService productService;
    private final ApplicationContext springContext;

    @FXML private FlowPane productsFlowPane;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageInfoLabel;

    private int currentPage = 0;
    private final int pageSize = 8;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        loadInventoryData();
    }

    public void loadInventoryData() {
        Runnable fetchTask = () -> {
            try {
                Page<ProductResponse> productPage = productService.getStorage(currentPage, pageSize);

                Platform.runLater(() -> {
                    totalPages = productPage.getTotalPages();
                    updateUI(productPage);
                });
            } catch (Exception e) {
                Platform.runLater(() -> pageInfoLabel.setText("Failed to load inventory assets."));
            }
        };

        Thread thread = new Thread(new DelegatingSecurityContextRunnable(fetchTask));
        thread.setDaemon(true);
        thread.start();
    }

    private void updateUI(Page<ProductResponse> page) {
        productsFlowPane.getChildren().clear();

        if (page.isEmpty()) {
            Label blankLabel = new Label("Your storage vault is empty. Click '+ Add Product' to start registry listings.");
            blankLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8; -fx-padding: 40 0 0 0;");
            productsFlowPane.getChildren().add(blankLabel);
        } else {
            for (ProductResponse product : page.getContent()) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/views/account/inventory/ProductItem.fxml"));
                    loader.setControllerFactory(springContext::getBean);
                    Parent itemNode = loader.load();

                    ProductItemController controller = loader.getController();

                    // 1. Pass the secondary callback to handle the green button specific launch sequence
                    controller.setProductData(product, clickedProduct -> openCreateAuctionModal(clickedProduct));

                    // 2. Clicking anywhere else on the card opens up the product workspace manager
                    itemNode.setOnMouseClicked(event -> openProductManagementConsole(product));

                    productsFlowPane.getChildren().add(itemNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        pageInfoLabel.setText(String.format("Page %d of %d", currentPage + 1, Math.max(totalPages, 1)));
        prevButton.setDisable(currentPage <= 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    /**
     * Spawns the CreateAuctionBox modal when clicking the card button.
     */
    private void openCreateAuctionModal(ProductResponse product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/views/account/inventory/CreateAuctionBox.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            CreateAuctionBoxController controller = loader.getController();
            controller.initializeData(product, this::loadInventoryData);

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setTitle("Auction Creation Wizard");
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();

        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to load CreateAuctionBox.fxml -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Spawns the ProductBox modal console for updating/deleting when clicking the card body.
     */
    private void openProductManagementConsole(ProductResponse product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/views/account/inventory/ProductBox.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            ProductBoxController controller = loader.getController();
            controller.initializeProductData(product, this::loadInventoryData);

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setTitle("Product Management Workspace Console");
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();

        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to load ProductBox.fxml -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void openAddProductModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/views/account/inventory/AddProductBox.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            AddProductBoxController controller = loader.getController();
            controller.setOnSuccess(this::loadInventoryData);

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setTitle("Inventory Storage Wizard");
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadInventoryData();
        }
    }

    @FXML
    public void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadInventoryData();
        }
    }
}
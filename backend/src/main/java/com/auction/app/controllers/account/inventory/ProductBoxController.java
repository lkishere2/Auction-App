package com.auction.app.controllers.account.inventory;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.auction.app.domains.products.ProductService;
import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import com.auction.app.domains.products.model.Tag;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductBoxController {

    private final ProductService productService;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField quantityField;
    @FXML private ComboBox<Tag> tagComboBox;
    @FXML private TextField fileDestinationField;
    @FXML private Label errorLabel;

    private Long productId;
    private String currentImageUrl;
    private File selectedImageFile;
    private Runnable onRefreshCallback;

    @FXML
    public void initialize() {
        tagComboBox.setItems(FXCollections.observableArrayList(Tag.values()));
        clearError();
        selectedImageFile = null;
    }

    public void initializeProductData(ProductResponse product, Runnable refreshCallback) {
        this.productId = product.getId();
        this.currentImageUrl = product.getProductImageUrl();
        this.onRefreshCallback = refreshCallback;

        nameField.setText(product.getProductName());
        descriptionArea.setText(product.getDescription());
        quantityField.setText(String.valueOf(product.getQuantity()));

        if (product.getTags() != null && !product.getTags().isEmpty()) {
            tagComboBox.setValue(product.getTags().iterator().next());
        } else {
            tagComboBox.setValue(Tag.OTHER);
        }
    }

    @FXML
    public void handleFileChooseAction() {
        if (nameField.getScene() == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Replace Product Image File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );

        Stage activeStage = (Stage) nameField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(activeStage);

        if (file != null) {
            this.selectedImageFile = file;
            this.fileDestinationField.setText(file.getName());
            clearError();
        }
    }

    @FXML
    public void handleUpdate() {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String quantityRaw = quantityField.getText().trim();
        Tag selectedTag = tagComboBox.getValue();

        if (name.isEmpty() || quantityRaw.isEmpty()) {
            showError("Product Name and Stock Quantity fields are required!");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityRaw);
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Quantity field must be a valid positive integer!");
            return;
        }

        ProductRequest request = new ProductRequest();
        request.setProductName(name);
        request.setDescription(description);
        request.setQuantity(quantity);
        request.setTags(Set.of(selectedTag));
        request.setProductImageUrl(currentImageUrl);

        showError("Uploading image and updating product fields...");

        // FIX: Wrap the task using DelegatingSecurityContextRunnable to forward the user session context securely
        Runnable updateTask = () -> {
            try {
                if (selectedImageFile != null) {
                    Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                            "cloud_name", cloudName,
                            "api_key", apiKey,
                            "api_secret", apiSecret,
                            "secure", true
                    ));

                    Map uploadResult = cloudinary.uploader().upload(selectedImageFile, ObjectUtils.emptyMap());
                    String remoteUrl = (String) uploadResult.get("secure_url");
                    request.setProductImageUrl(remoteUrl);
                }

                // Authorized update service execution pass
                productService.editProduct(productId, request);

                // Update visual frames safely inside the JavaFX lifecycle queue
                Platform.runLater(this::notifyAndClose);

            } catch (Exception ex) {
                Platform.runLater(() -> showError("Update action rejected: " + ex.getMessage()));
            }
        };

        Thread worker = new Thread(new DelegatingSecurityContextRunnable(updateTask));
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    public void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Permanently Delete Item?");
        alert.setContentText("Are you sure you want to remove this product from your repository inventory?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            showError("Deleting item record from database storage...");

            // FIX: Wrap deletion inside context container tasks as well
            Runnable deleteTask = () -> {
                try {
                    productService.deleteProduct(productId);
                    Platform.runLater(this::notifyAndClose);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("Deletion failed: " + ex.getMessage()));
                }
            };

            Thread worker = new Thread(new DelegatingSecurityContextRunnable(deleteTask));
            worker.setDaemon(true);
            worker.start();
        }
    }

    @FXML
    public void handleCancel() {
        closeWindow();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void notifyAndClose() {
        if (onRefreshCallback != null) {
            onRefreshCallback.run();
        }
        closeWindow();
    }

    private void closeWindow() {
        if (nameField.getScene() != null) {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        }
    }
}
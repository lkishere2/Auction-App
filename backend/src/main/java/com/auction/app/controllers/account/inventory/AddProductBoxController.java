package com.auction.app.controllers.account.inventory;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.auction.app.domains.products.ProductService;
import com.auction.app.domains.products.dtos.ProductRequest;
import com.auction.app.domains.products.model.Tag;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AddProductBoxController {

    private final ProductService productService;

    // Inject your application.properties credentials exactly like the profile upload
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

    private File selectedImageFile;
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        tagComboBox.setItems(FXCollections.observableArrayList(Tag.values()));
        tagComboBox.setValue(Tag.OTHER);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        selectedImageFile = null;
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void handleFileChooseAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image Asset");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );

        Stage activeStage = (Stage) nameField.getScene().getWindow();
        File destination = fileChooser.showOpenDialog(activeStage);

        if (destination != null) {
            this.selectedImageFile = destination;
            this.fileDestinationField.setText(destination.getName());
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    @FXML
    public void handleSave() {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String quantityRaw = quantityField.getText().trim();
        Tag selectedTag = tagComboBox.getValue();

        // 1. Structural Form Input Fields Validations
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

        // 2. Cloudinary Upload Processing Pipeline Execution
        if (selectedImageFile != null) {
            try {
                // Initialize configuration instance map matching your profiles design
                Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true
                ));

                // Execute the cloud stream upload task over the network
                Map uploadResult = cloudinary.uploader().upload(selectedImageFile, ObjectUtils.emptyMap());
                String remoteCloudinarySecureUrl = (String) uploadResult.get("secure_url");

                // Assign cloud link directly to product payload model
                request.setProductImageUrl(remoteCloudinarySecureUrl);

            } catch (Exception ex) {
                showError("Cloudinary cloud stream break: " + ex.getMessage());
                return; // Kill sequence tracking to prevent blank database listings
            }
        }

        // 3. Database Injection State Save Pass
        try {
            productService.addProduct(request);
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            closeWindow();
        } catch (Exception ex) {
            showError("Database rejection entry: " + ex.getMessage());
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

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
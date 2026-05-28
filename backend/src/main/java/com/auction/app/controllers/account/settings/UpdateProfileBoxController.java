package com.auction.app.controllers.account.settings;

import com.auction.app.domains.users.users.UserController;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.ProfileImageRequest;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class UpdateProfileBoxController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;

    @FXML private TextField fileSelectedPathField;
    @FXML private Button uploadImageButton;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField passwordField;

    @Autowired
    private UserController userController;

    @Value("${cloudinary.cloud-name}") private String cloudName;
    @Value("${cloudinary.api-key}") private String apiKey;
    @Value("${cloudinary.api-secret}") private String apiSecret;

    private File localSelectedImageFile;
    private Runnable onUpdateSuccessCallback;

    @FXML
    public void initialize() {
        loadCurrentUserData();
    }

    public void setOnUpdateSuccess(Runnable callback) {
        this.onUpdateSuccessCallback = callback;
    }

    /**
     * Pre-populates the input fields using the live database properties from UserResponse.
     */
    private void loadCurrentUserData() {
        try {
            ResponseEntity<UserResponse> response = userController.getCurrentUserInformation();
            if (response != null && response.getBody() != null) {
                UserResponse userInfo = response.getBody();
                usernameField.setText(userInfo.getUsername());
                emailField.setText(userInfo.getEmail());
                currentPasswordField.clear();
                passwordField.clear();
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize UpdateProfileBox from live database info endpoint: " + e.getMessage());
        }
    }

    @FXML
    private void handleUsernameUpdate() {
        try {
            UsernameRequest req = new UsernameRequest();
            req.setUsername(usernameField.getText());

            userController.updateUsername(req);
            showSuccessNotification("Username Changed", "Your profile account nickname synchronized completely.");
        } catch (Exception ex) {
            showErrorNotification("Username Error", ex.getMessage());
        }
    }

    @FXML
    private void handleEmailUpdate() {
        try {
            EmailRequest req = new EmailRequest();
            req.setEmail(emailField.getText());

            userController.updateEmail(req);
            showSuccessNotification("Email Saved", "System data records point safely to your new email.");
        } catch (Exception ex) {
            showErrorNotification("Email Error", ex.getMessage());
        }
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Image Asset");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = chooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            this.localSelectedImageFile = selectedFile;
            fileSelectedPathField.setText(selectedFile.getAbsolutePath());
            uploadImageButton.setDisable(false);
        }
    }

    @FXML
    private void handleImageUpload() {
        if (localSelectedImageFile == null) return;

        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));

            Map uploadResult = cloudinary.uploader().upload(localSelectedImageFile, ObjectUtils.emptyMap());
            String remoteCloudinarySecureUrl = (String) uploadResult.get("secure_url");

            ProfileImageRequest req = new ProfileImageRequest();
            req.setProfileImageUrl(remoteCloudinarySecureUrl);
            userController.updateProfileImage(req);

            showSuccessNotification("Avatar Updated", "Your high-resolution avatar file processed successfully!");

            fileSelectedPathField.clear();
            uploadImageButton.setDisable(true);
            this.localSelectedImageFile = null;
        } catch (Exception ex) {
            showErrorNotification("Upload Failure", "Cloudinary cloud stream break: " + ex.getMessage());
        }
    }

    @FXML
    private void handlePasswordUpdate() {
        if (currentPasswordField.getText().isBlank() || passwordField.getText().isBlank()) {
            showErrorNotification("Validation Blank", "You must populate both fields to change credentials.");
            return;
        }

        try {
            PasswordRequest req = new PasswordRequest();
            req.setCurrentPassword(currentPasswordField.getText());
            req.setNewPassword(passwordField.getText());

            userController.updatePassword(req);

            showSuccessNotification("Password Changed", "Security parameters updated cleanly.");
            currentPasswordField.clear();
            passwordField.clear();
        } catch (Exception ex) {
            showErrorNotification("Security Sync Error", ex.getMessage());
        }
    }

    private void showSuccessNotification(String title, String context) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(context);
        alert.showAndWait();

        // Run UI dynamic callback hook routines instantly to refresh the visual header cards
        if (onUpdateSuccessCallback != null) {
            onUpdateSuccessCallback.run();
        }
    }

    private void showErrorNotification(String title, String reason) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Transaction Rejected");
        alert.setContentText(reason);
        alert.showAndWait();
    }
}
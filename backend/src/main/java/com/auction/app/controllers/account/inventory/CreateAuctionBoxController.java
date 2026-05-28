package com.auction.app.controllers.account.inventory;

import com.auction.app.domains.auction.auction.AuctionService;
import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.products.dtos.ProductResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class CreateAuctionBoxController {

    private final AuctionService auctionService;

    @FXML private Label productInfoSubtitle;
    @FXML private TextField quantityField;
    @FXML private TextField priceField;

    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourBox;
    @FXML private ComboBox<String> startMinuteBox;

    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourBox;
    @FXML private ComboBox<String> endMinuteBox;

    @FXML private Label errorLabel;

    private Long targetProductId;
    private int maxQuantity;
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        // Initialize Time Combo Boxes (00-23 hours, 00-59 minutes)
        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) hours[i] = String.format("%02d", i);

        String[] minutes = {"00", "15", "30", "45"};

        startHourBox.setItems(FXCollections.observableArrayList(hours));
        endHourBox.setItems(FXCollections.observableArrayList(hours));
        startMinuteBox.setItems(FXCollections.observableArrayList(minutes));
        endMinuteBox.setItems(FXCollections.observableArrayList(minutes));

        startHourBox.setValue("12"); startMinuteBox.setValue("00");
        endHourBox.setValue("12"); endMinuteBox.setValue("00");

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void initializeData(ProductResponse product, Runnable successCallback) {
        this.targetProductId = product.getId(); // Captures the unique database primary key ID
        this.maxQuantity = product.getQuantity();
        this.onSuccessCallback = successCallback;

        productInfoSubtitle.setText(String.format("Configuring market parameters for: %s (Max Qty: %d)",
                product.getProductName(), maxQuantity));

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(3));
    }

    @FXML
    public void handleCreateAuction() {
        try {
            // 1. Validate Numeric Inputs
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0 || quantity > maxQuantity) {
                showError("Quantity must be between 1 and " + maxQuantity);
                return;
            }

            BigDecimal startingPrice = new BigDecimal(priceField.getText().trim());
            if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Starting price must be greater than 0");
                return;
            }

            // 2. Safely parse JavaFX UI Times into UTC Instants
            Instant startInstant = parseTime(startDatePicker.getValue(), startHourBox.getValue(), startMinuteBox.getValue());
            Instant endInstant = parseTime(endDatePicker.getValue(), endHourBox.getValue(), endMinuteBox.getValue());

            if (startInstant == null || endInstant == null) {
                showError("Please fill out all Date and Time fields correctly.");
                return;
            }
            if (!endInstant.isAfter(startInstant)) {
                showError("End Time must be strictly after the Start Time.");
                return;
            }

            // FIXED: Explicitly passing targetProductId as the first parameter matching your AuctionRequest DTO definition
            AuctionRequest request = new AuctionRequest(
                    targetProductId,
                    quantity,
                    startingPrice,
                    startInstant,
                    endInstant
            );

            // 4. Execute Backend Creation Thread
            showError("Launching auction network event...");
            Runnable createAction = () -> {
                try {
                    auctionService.createAuction(request);
                    Platform.runLater(this::notifyAndClose);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("Validation Rejected: " + ex.getMessage()));
                }
            };

            Thread worker = new Thread(new DelegatingSecurityContextRunnable(createAction));
            worker.setDaemon(true);
            worker.start();

        } catch (NumberFormatException e) {
            showError("Please enter valid numeric values for Quantity and Price.");
        }
    }

    private Instant parseTime(LocalDate date, String hourStr, String minuteStr) {
        if (date == null || hourStr == null || minuteStr == null) return null;
        int hour = Integer.parseInt(hourStr);
        int minute = Integer.parseInt(minuteStr);
        return LocalDateTime.of(date, LocalTime.of(hour, minute))
                .atZone(ZoneId.systemDefault())
                .toInstant();
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

    private void notifyAndClose() {
        if (onSuccessCallback != null) onSuccessCallback.run();
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) quantityField.getScene().getWindow();
        stage.close();
    }
}
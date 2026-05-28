package com.auction.app.controllers.market;

import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.model.AuctionStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// FIX #1: Added @Component so Spring manages this controller.
// MarketSearchBar.fxml is loaded via <fx:include> inside MarketView.fxml,
// whose fx:controller (MarketViewController) is a Spring bean. JavaFX will
// ask the Spring controller factory to resolve this controller by type —
// without @Component it is not registered in the context, causing the
// NoSuchBeanDefinitionException that crashes the entire MarketView load.
@Component
public class MarketSearchBarController {
    @FXML private TextField searchField;
    @FXML private TextField minPriceField;
    @FXML private ComboBox<AuctionStatus> statusComboBox;
    @FXML private Button searchButton;

    private Runnable searchTriggerCallback;

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList(AuctionStatus.values()));
        statusComboBox.setValue(AuctionStatus.ACTIVE);

        minPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                minPriceField.setText(oldVal);
            }
        });
    }

    public void setOnSearchTriggered(Runnable callback) {
        this.searchTriggerCallback = callback;
    }

    @FXML
    private void handleSearchAction() {
        if (searchTriggerCallback != null) {
            searchTriggerCallback.run();
        }
    }

    public AuctionFindingRequest getQueryRequest() {
        AuctionFindingRequest request = new AuctionFindingRequest();
        String query = searchField.getText().trim();
        request.setProductName(query.isEmpty() ? null : query);

        String priceStr = minPriceField.getText().trim();
        if (!priceStr.isEmpty()) {
            try {
                request.setMinStartingPrice(new BigDecimal(priceStr));
            } catch (Exception e) {
                request.setMinStartingPrice(BigDecimal.ZERO);
            }
        } else {
            request.setMinStartingPrice(BigDecimal.ZERO);
        }

        request.setStatus(statusComboBox.getValue());
        return request;
    }
}
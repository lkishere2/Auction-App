package com.auction.app.controllers.market;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.math.BigDecimal;
import java.util.function.Consumer;

public class MarketAuctionItemController {
    @FXML private Label statusTag;
    @FXML private Label titleLabel;
    @FXML private Label priceLabel;

    private AuctionResponse currentAuction;
    private Consumer<AuctionResponse> onClickHandler;

    public void populate(AuctionResponse auction, Consumer<AuctionResponse> handler) {
        this.currentAuction = auction;
        this.onClickHandler = handler;

        titleLabel.setText(auction.getProductName() != null ? auction.getProductName() : "Unnamed Product");
        statusTag.setText(auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN");

        BigDecimal activePrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartingPrice();
        if (activePrice == null) activePrice = BigDecimal.ZERO;
        priceLabel.setText("$" + String.format("%.2f", activePrice));
    }

    @FXML
    private void handleCardClick() {
        if (onClickHandler != null && currentAuction != null) {
            onClickHandler.accept(currentAuction);
        }
    }
}
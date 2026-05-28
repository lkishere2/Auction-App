package com.auction.app.controllers.market;

import com.auction.app.domains.auction.auction.AuctionService;
import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MarketViewController {

    @FXML private MarketSearchBarController searchBarController;
    @FXML private MarketAuctionGridController auctionGridController;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageTrackerLabel;

    @Autowired
    private AuctionService auctionService;

    private int currentPage = 0;
    private final int pageSize = 12;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        if (searchBarController != null) {
            searchBarController.setOnSearchTriggered(() -> {
                this.currentPage = 0;
                loadMarketDataPage();
            });
        }
        loadMarketDataPage();
    }

    private void loadMarketDataPage() {
        AuctionFindingRequest criteria = searchBarController != null ?
                searchBarController.getQueryRequest() : new AuctionFindingRequest();

        Pageable paginationParams = PageRequest.of(currentPage, pageSize);

        Thread backgroundWorker = new Thread(() -> {
            try {
                Page<AuctionResponse> outcomePage = auctionService.getDiscoverableAuctions(criteria, paginationParams);

                Platform.runLater(() -> {
                    this.totalPages = Math.max(1, outcomePage.getTotalPages());

                    if (auctionGridController != null) {
                        auctionGridController.renderItems(outcomePage.getContent(), this::handleProductRedirect);
                    }

                    pageTrackerLabel.setText(String.format("Page %d of %d", currentPage + 1, totalPages));
                    prevButton.setDisable(currentPage == 0);
                    nextButton.setDisable(currentPage >= totalPages - 1);
                });
            } catch (Exception err) {
                err.printStackTrace();
            }
        });
        backgroundWorker.setDaemon(true);
        backgroundWorker.start();
    }

    @FXML private void handlePrevPage() { if (currentPage > 0) { currentPage--; loadMarketDataPage(); } }
    @FXML private void handleNextPage() { if (currentPage < totalPages - 1) { currentPage++; loadMarketDataPage(); } }

    private void handleProductRedirect(AuctionResponse selectedProduct) {
        System.out.println("Forwarding action context to view item UUID: " + selectedProduct.getId());
    }
}
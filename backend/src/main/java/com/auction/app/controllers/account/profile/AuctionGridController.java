package com.auction.app.controllers.account.profile;

import com.auction.app.domains.auction.auction.AuctionService;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuctionGridController {

    private final AuctionService auctionService;
    private final ApplicationContext springContext;

    @FXML private FlowPane gridFlowPane;
    @FXML private Button prevGridBtn;
    @FXML private Button nextGridBtn;
    @FXML private Label gridPageLabel;

    private int currentPage = 0;
    private final int pageSize = 9; // Perfect 3x3 square block pattern alignment layout
    private int totalPages = 1;

    public void loadUserGridFeeds() {
        // Enforce Spring Security thread context forwarding explicitly
        Runnable fetchTask = () -> {
            try {
                Pageable pageable = PageRequest.of(currentPage, pageSize);

                // Fetch paginated responses from your unmodified AuctionService
                Page<AuctionResponse> postPage = auctionService.getMyAuctions(pageable);

                // Safely update the JavaFX Scene Graph layout elements
                Platform.runLater(() -> {
                    if (postPage != null) {
                        this.totalPages = postPage.getTotalPages();
                        populateGrid(postPage);
                    } else {
                        renderEmptyState();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    // Log out any remaining backend session exceptions to console stdout
                    System.err.println("JavaFX Worker Thread -> Failed to load auction items cleanly:");
                    ex.printStackTrace();

                    gridPageLabel.setText("Failed to load gallery.");
                    renderEmptyState();
                });
            }
        };

        Thread worker = new Thread(new DelegatingSecurityContextRunnable(fetchTask));
        worker.setDaemon(true);
        worker.start();
    }

    private void populateGrid(Page<AuctionResponse> page) {
        gridFlowPane.getChildren().clear();

        if (page.isEmpty()) {
            renderEmptyState();
        } else {
            for (AuctionResponse auction : page.getContent()) {
                try {
                    // Direct dynamic resolution matching your verified FXML resource locations
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/views/account/profile/AuctionItem.fxml"));
                    loader.setControllerFactory(springContext::getBean);
                    Parent itemTile = loader.load();

                    AuctionItemController controller = loader.getController();
                    controller.setAuctionPostData(auction);

                    gridFlowPane.getChildren().add(itemTile);
                } catch (IOException e) {
                    System.err.println("FXML Parsing Dropped out for item template file: " + e.getMessage());
                }
            }
        }

        // Refresh Navigation Layout States parameters safely
        gridPageLabel.setText(String.format("Page %d of %d", currentPage + 1, Math.max(totalPages, 1)));
        prevGridBtn.setDisable(currentPage <= 0);
        nextGridBtn.setDisable(currentPage >= totalPages - 1);
    }

    private void renderEmptyState() {
        gridFlowPane.getChildren().clear();
        Label noPostsLabel = new Label("No auction posts shared yet.");
        noPostsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-padding: 30 0 0 10;");
        gridFlowPane.getChildren().add(noPostsLabel);
    }

    @FXML
    public void handlePreviousGridPage() {
        if (currentPage > 0) {
            currentPage--;
            loadUserGridFeeds();
        }
    }

    @FXML
    public void handleNextGridPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadUserGridFeeds();
        }
    }
}
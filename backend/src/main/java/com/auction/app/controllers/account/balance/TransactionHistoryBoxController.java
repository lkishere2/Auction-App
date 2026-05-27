package com.auction.app.controllers.account.balance;

import com.auction.app.domains.transaction.TransactionController;
import com.auction.app.domains.transaction.dtos.TransactionResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;

@Component
public class TransactionHistoryBoxController {

    @FXML private TableView<TransactionResponse> transactionTable;
    @FXML private TableColumn<TransactionResponse, String> typeColumn;
    @FXML private TableColumn<TransactionResponse, String> amountColumn;
    @FXML private TableColumn<TransactionResponse, String> statusColumn;
    @FXML private TableColumn<TransactionResponse, String> dateColumn;

    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageIndexIndicatorLabel;

    @Autowired private TransactionController transactionController;

    private BalanceViewController parentController;

    private final ObservableList<TransactionResponse> tableItems = FXCollections.observableArrayList();
    private int currentPageIndex = 0;
    private final int pageSizeLimit = 5;
    private int totalPagesCount = 1;

    public void setParentController(BalanceViewController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing User Transaction Ledger Table dynamic element display...");

        typeColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getType().name()));
        amountColumn.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("$%,.2f", c.getValue().getAmount())));
        statusColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        dateColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCreatedAt().toString()));

        if (transactionTable != null) {
            transactionTable.setItems(tableItems);
            transactionTable.setPlaceholder(new Label("No transactions found in user history records."));
        }

        loadHistoricalPageData();
    }

    public void loadHistoricalPageData() {
        Runnable task = new DelegatingSecurityContextRunnable(() -> {
            try {
                ResponseEntity<Page<TransactionResponse>> response =
                        transactionController.getUserTransactions(currentPageIndex, pageSizeLimit);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Page<TransactionResponse> pageData = response.getBody();

                    Platform.runLater(() -> {
                        tableItems.setAll(pageData.getContent());
                        totalPagesCount = Math.max(1, pageData.getTotalPages());
                        updatePaginationControls();
                    });
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch historical pages out of core ledger records.");
                e.printStackTrace();
            }
        });
        new Thread(task).start();
    }

    private void updatePaginationControls() {
        if (pageIndexIndicatorLabel != null) {
            pageIndexIndicatorLabel.setText(String.format("Page %d of %d", currentPageIndex + 1, totalPagesCount));
        }
        if (previousPageButton != null) previousPageButton.setDisable(currentPageIndex == 0);
        if (nextPageButton != null) nextPageButton.setDisable(currentPageIndex >= totalPagesCount - 1);
    }

    @FXML
    private void loadPreviousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            loadHistoricalPageData();
        }
    }

    @FXML
    private void loadNextPage() {
        if (currentPageIndex < totalPagesCount - 1) {
            currentPageIndex++;
            loadHistoricalPageData();
        }
    }
}
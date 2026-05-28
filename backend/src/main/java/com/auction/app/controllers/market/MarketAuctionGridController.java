package com.auction.app.controllers.market;

import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component("marketAuctionGridController")
public class MarketAuctionGridController {
    @FXML private GridPane gridPane;

    public void renderItems(List<AuctionResponse> items, Consumer<AuctionResponse> onBidClicked) {
        gridPane.getChildren().clear();
        if (items == null) return;

        int columnLimit = 4;
        for (int i = 0; i < items.size(); i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/views/market/MarketAuctionItem.fxml"));
                MarketAuctionItemController itemController = new MarketAuctionItemController();
                loader.setController(itemController);

                Node cardNode = loader.load();
                itemController.populate(items.get(i), onBidClicked);

                int row = i / columnLimit;
                int col = i % columnLimit;
                gridPane.add(cardNode, col, row);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
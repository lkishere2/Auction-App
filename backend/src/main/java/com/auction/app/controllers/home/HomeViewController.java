package com.auction.app.controllers.home;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class HomeViewController {

    @FXML
    private BorderPane homeViewContainer;

    @FXML
    private StackPane contentViewPane;

    @FXML
    public void initialize() {
        // This initializes cleanly when JavaFX loads HomeView.fxml.
        // We leave the content pane empty for now, ready for feature panels later.
        System.out.println("HomeView Dashboard Frame mounted successfully!");
    }
}
package com.auction.app.controllers.account;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class AccountViewController {

    @FXML
    private StackPane accountContentCanvas;

    @FXML
    public void initialize() {
        System.out.println("AccountView Container shell layout successfully initialized.");
        // The canvas is cleanly preserved as an empty, blank view just like you asked!
    }

    public StackPane getAccountContentCanvas() {
        return accountContentCanvas;
    }
}
package com.auction.app.controllers;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class LoginViewController {

    // JavaFX auto-wires the inner component controller using [fx:id] + "Controller" naming matching rules
    @FXML
    private LoginBoxController loginBoxController;

    @FXML
    public void initialize() {
        System.out.println("Parent LoginViewController initialized leveraging direct AuthController orchestration.");
    }
}
package com.auction.app.controllers.auth;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class RegisterViewController {

    @FXML
    private RegisterBoxController registerBoxController;

    @FXML
    public void initialize() {
        System.out.println("Parent RegisterViewController mounted gracefully with atomic children components.");
    }
}
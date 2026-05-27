package com.auction.app.controllers;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class ForgetPasswordViewController {

    @FXML
    private ForgetPasswordBoxController forgetPasswordBoxController;

    @FXML
    public void initialize() {
        System.out.println("Parent ForgetPasswordViewController initialized successfully.");
    }
}
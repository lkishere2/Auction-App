package com.auction.app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainController {

    @FXML
    private StackPane rootViewContainer;

    @Autowired
    private ApplicationContext springContext;

    @FXML
    public void initialize() {
        navigateTo("/ui/LoginView.fxml");
    }


    public void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            loader.setControllerFactory(springContext::getBean);

            Parent targetView = loader.load();

            rootViewContainer.getChildren().setAll(targetView);

        } catch (IOException e) {
            System.err.println("Navigation Routing Pipeline Failure: Unable to parse FXML view map at " + fxmlPath);
            e.printStackTrace();
        }
    }
}
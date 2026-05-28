package com.auction.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ConfigurableApplicationContext applicationContext;

    public StageInitializer(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            Stage stage = event.getStage();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/main.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent parent = fxmlLoader.load();

            stage.setScene(new Scene(parent, 800, 600));
            stage.setTitle("Auction Application");
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load main FXML file", e);
        }
    }
}
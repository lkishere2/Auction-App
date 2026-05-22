package com.auction.app.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void showAuth() {
        loadScene("/pages/authpage.fxml", 1000, 700, "BidVault");
    }

    public static void showDashboard() {
        loadScene("/pages/dashboard.fxml", 1100, 700, "BidVault — Dashboard");
    }

    public static void showInventory() {
        loadScene("/pages/inventory.fxml", 1000, 700, "BidVault — Inventory");
    }

    private static void loadScene(String fxmlPath, int width, int height, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(SceneManager.class.getResource("/styles.css").toExternalForm());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
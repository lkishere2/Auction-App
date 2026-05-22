package com.auction.app;

import com.auction.app.ui.SceneManager;
import javafx.stage.Stage;

public class FxWindow {
    public void show() {
        Stage stage = new Stage();
        SceneManager.init(stage);
        SceneManager.showDashboard();
    }
}
package com.auction.app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
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

    // Tracks the inner context region container area inside your Home/Dashboard view frame
    private Pane activeWorkspaceArea;

    @FXML
    public void initialize() {
        navigateTo("/ui/views/auth/LoginView.fxml");
    }

    /**
     * Sets the workspace container reference where sub-panels
     * are dynamically swapped in while retaining the outer layout framework shell.
     */
    public void setActiveWorkspaceArea(Pane workspaceArea) {
        this.activeWorkspaceArea = workspaceArea;
    }

    /**
     * Replaces the absolute main viewing container scene node context graph window.
     */
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

    /**
     * FIXED BRIDGE METHOD: Injects a pre-loaded node graph view directly
     * into the interior center layout area tracked by your active application state workspace.
     */
    public void displayInWorkspace(Parent viewNode) {
        if (activeWorkspaceArea != null) {
            // Clears out previous internal panel dashboard cards and updates rendering to the new node
            activeWorkspaceArea.getChildren().setAll(viewNode);
        } else if (rootViewContainer != null) {
            // Fallback strategy: If no structural workspace is registered yet, render full screen inside main stack
            System.out.println("Workspace pane tracking layer unassigned. Defaulting rendering tree up to main stack panel region.");
            rootViewContainer.getChildren().setAll(viewNode);
        } else {
            System.err.println("Critical Error: Core application rendering target anchors are completely unavailable!");
        }
    }
}
package com.auction.app.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype") // Crucial: Allows Spring to spawn unique instances for each sidebar tab include
public class NavbarItemController {

    @FXML
    private HBox navItemRoot; // Maps to fx:id="navItemRoot" in NavItem.fxml

    @FXML
    private Label iconLabel; // Maps to fx:id="iconLabel" in NavItem.fxml

    @FXML
    private Label itemTextLabel; // Maps to fx:id="itemTextLabel" in NavItem.fxml

    @FXML
    public void initialize() {
        // Hover effects to make the navigation elements feel responsive
        if (navItemRoot != null) {
            navItemRoot.setOnMouseEntered(event -> navItemRoot.setStyle(
                    "-fx-background-color: #334155; -fx-cursor: hand; -fx-padding: 10 15 10 15; -fx-background-radius: 6;"
            ));
            navItemRoot.setOnMouseExited(event -> navItemRoot.setStyle(
                    "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 10 15 10 15; -fx-background-radius: 6;"
            ));
        }
    }

    /**
     * Updates the text label displayed inside the navigation sub-component dynamically.
     */
    public void setItemText(String text) {
        if (itemTextLabel != null) {
            itemTextLabel.setText(text);
        }
    }

    /**
     * Optional utility helper to modify the text styling or inject glyph symbols later.
     */
    public void setIconText(String glyphText) {
        if (iconLabel != null) {
            iconLabel.setText(glyphText);
        }
    }
}
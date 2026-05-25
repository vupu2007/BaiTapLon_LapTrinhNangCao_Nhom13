package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class SidebarController {

    @FXML
    private VBox sidebar;

    private boolean isCollapsed = false;

    @FXML
    private void toggleSidebar() {
        if (isCollapsed) {
            sidebar.setPrefWidth(220); // mở rộng
        } else {
            sidebar.setPrefWidth(60);  // thu nhỏ
        }
        isCollapsed = !isCollapsed;
    }
}
package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class SidebarController {

    @FXML private VBox sidebar;

    // Danh sách các phần tử chứa chữ cần ẩn khi thu nhỏ sidebar (bổ sung tùy FXML của bạn)
    @FXML private Label lblRoleSidebar;
    @FXML private Button btnHome, btnWallet, btnAuction, btnSelling, btnHistory, btnSettings;

    private boolean isCollapsed = false;
    private Timeline toggleTimeline;

    @FXML
    public void initialize() {
        // Khởi tạo Timeline một lần để tối ưu bộ nhớ, tránh tạo mới liên tục
        toggleTimeline = new Timeline();
    }

    /**
     * 🚀 TOGGLE SIDEBAR: Tích hợp hiệu ứng nội suy kích thước (Interpolation Animation) cực mượt
     */
    @FXML
    private void toggleSidebar() {
        if (sidebar == null || toggleTimeline.getStatus() == Timeline.Status.RUNNING) return;

        // Đảo trạng thái ngay lập tức để đồng bộ
        isCollapsed = !isCollapsed;
        double targetWidth = isCollapsed ? 60.0 : 220.0;

        toggleTimeline.getKeyFrames().clear();
        toggleTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(200),
                new KeyValue(sidebar.prefWidthProperty(), targetWidth)));

        // Nếu thu nhỏ: Ẩn chữ ngay để animation mượt, không bị dính text
        if (isCollapsed) updateSidebarTextVisibility(false);

        toggleTimeline.setOnFinished(e -> {
            // Nếu mở rộng: Hiện chữ sau khi animation hoàn tất
            if (!isCollapsed) updateSidebarTextVisibility(true);
        });

        toggleTimeline.play();
    }
    /**
     * Hàm tiện ích quản lý ẩn hiện phần chữ của toàn bộ Menu Sidebar an toàn
     */
    private void updateSidebarTextVisibility(boolean visible) {
        // Không cần Platform.runLater vì đã chạy trên UI thread
        List<Button> menuButtons = List.of(btnHome, btnWallet, btnAuction, btnSelling, btnHistory, btnSettings);

        for (Button btn : menuButtons) {
            if (btn == null) continue;
            if (visible) {
                btn.setText(btn.getUserData() != null ? btn.getUserData().toString() : "");
                btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            } else {
                btn.setUserData(btn.getText());
                btn.setText("");
                btn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            }
        }
        if (lblRoleSidebar != null) {
            lblRoleSidebar.setVisible(visible);
            lblRoleSidebar.setManaged(visible);
        }
    }
}
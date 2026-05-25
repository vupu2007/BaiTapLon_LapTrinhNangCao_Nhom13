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
        if (sidebar == null) return;

        // Nếu hiệu ứng cũ đang chạy, dừng lại ngay để tránh xung đột luồng hiệu ứng
        if (toggleTimeline.getStatus() == Timeline.Status.RUNNING) {
            toggleTimeline.stop();
        }

        toggleTimeline.getKeyFrames().clear();

        double targetWidth = isCollapsed ? 220.0 : 60.0;

        // 1. Tạo hiệu ứng mượt mà chuyển đổi kích thước trong 200 mili-giây
        KeyValue widthValue = new KeyValue(sidebar.prefWidthProperty(), targetWidth);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(200), widthValue);
        toggleTimeline.getKeyFrames().add(keyFrame);

        // 2. Xử lý đồng bộ Ẩn/Hiện chữ ngay khi bắt đầu hoặc kết thúc hiệu ứng để tránh vỡ chữ
        if (!isCollapsed) {
            // Đang chuẩn bị THU NHỎ -> Ẩn chữ ngay để không bị vỡ giao diện khi co lại
            updateSidebarTextVisibility(false);
        }

        // Kích hoạt chạy hiệu ứng
        toggleTimeline.setOnFinished(e -> {
            if (isCollapsed) {
                // Đã MỞ RỘNG xong -> Hiện lại chữ đầy đủ
                updateSidebarTextVisibility(true);
            }
            // Đảo trạng thái đóng mở
            isCollapsed = !isCollapsed;
        });

        toggleTimeline.play();
    }

    /**
     * Hàm tiện ích quản lý ẩn hiện phần chữ của toàn bộ Menu Sidebar an toàn
     */
    private void updateSidebarTextVisibility(boolean visible) {
        Platform.runLater(() -> {
            // Gom các nút hiện có vào danh sách kiểm tra an toàn
            List<Button> menuButtons = List.of(
                    btnHome, btnWallet, btnAuction, btnSelling, btnHistory, btnSettings
            );

            for (Button btn : menuButtons) {
                if (btn != null) {
                    if (visible) {
                        // Trạng thái mở rộng: Hiện chữ, căn lùi trái (Left)
                        btn.setText(btn.getUserData() != null ? btn.getUserData().toString() : btn.getText());
                        btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                    } else {
                        // Trạng thái thu nhỏ: Sao lưu chữ vào UserData, xóa text, chỉ giữ lại Icon căn giữa (Graphic)
                        if (btn.getText() != null && !btn.getText().isEmpty()) {
                            btn.setUserData(btn.getText()); // Lưu tạm chữ lại
                        }
                        btn.setText("");
                        btn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                    }
                }
            }

            // Ẩn nhãn tiêu đề vai trò (Role Label)
            if (lblRoleSidebar != null) {
                lblRoleSidebar.setVisible(visible);
                lblRoleSidebar.setManaged(visible);
            }
        });
    }
}
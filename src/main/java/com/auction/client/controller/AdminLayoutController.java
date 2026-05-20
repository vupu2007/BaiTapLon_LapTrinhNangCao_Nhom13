package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class AdminLayoutController {

    private static AdminLayoutController instance;

    @FXML private StackPane adminContentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnUserMgmt;

    @FXML
    public void initialize() {
        instance = this;
        // Mặc định tự động mở tab quản lý thành viên khi vừa đăng nhập vào admin
        openUserMgmt();
    }

    public static AdminLayoutController getInstance() {
        return instance;
    }

    /**
     * Nạp động các trang con giao diện vào vùng trung tâm bên phải
     */
    private FXMLLoader loadAdminPage(String fxmlPath) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                System.err.println("❌ Không tìm thấy file tại đường dẫn: " + fxmlPath);
                return null;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Node page = loader.load();
            adminContentArea.getChildren().clear();

            if (page instanceof Region) {
                Region region = (Region) page;
                region.prefWidthProperty().bind(adminContentArea.widthProperty());
                region.prefHeightProperty().bind(adminContentArea.heightProperty());
            }
            StackPane.setAlignment(page, Pos.TOP_CENTER);
            adminContentArea.getChildren().add(page);
            return loader;
        } catch (IOException e) {
            System.err.println("❌ Lỗi nạp cấu trúc FXML của trang con: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * HOÁN ĐỔI CLASS CSS CHUẨN:
     * Loại bỏ hoàn toàn inline-style bằng code Java, ép nút chạy chuẩn class css của trang chủ.
     */
    private void setButtonActive(Button activeButton) {
        Button[] buttons = {btnDashboard, btnUserMgmt};

        for (Button btn : buttons) {
            if (btn != null) {
                // Xóa bỏ trạng thái active cũ, đưa nút về giao diện nền trong suốt chữ xám mặc định
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
            }
        }

        if (activeButton != null) {
            // Đổi nút được bấm sang class active (Sáng xanh dương bo góc mượt mà)
            activeButton.getStyleClass().remove("nav-button");
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    @FXML
    private void openDashboard() {
        setButtonActive(btnDashboard);
        // Bổ sung nạp file FXML thống kê tổng quan tại đây nếu có trong tương lai
        System.out.println("-> Mở giao diện Thống kê Tổng quan");
    }

    @FXML
    private void openUserMgmt() {
        setButtonActive(btnUserMgmt);

        // Quét tìm file trang quản lý user ở cả 2 đường dẫn dự phòng tránh lỗi biên dịch Maven
        FXMLLoader res = loadAdminPage("/view/admin/AdminUserMgmtView.fxml");
        if (res == null) {
            loadAdminPage("/view/AdminUserMgmtView.fxml");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) adminContentArea.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("❌ Không thể điều hướng quay lại màn hình đăng nhập.");
            e.printStackTrace();
        }
    }
}
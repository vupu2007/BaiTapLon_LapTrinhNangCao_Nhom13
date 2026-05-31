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
     * 🌟 CẢI TIẾN: Hàm nạp động hỗ trợ cơ chế quét nhiều đường dẫn dự phòng liên tiếp.
     * Tự động duyệt qua các đường dẫn truyền vào, nạp file đầu tiên tìm thấy.
     */
    private FXMLLoader loadAdminPage(String... fxmlPaths) {
        URL fxmlLocation = null;
        String chosenPath = "";

        // Thử quét tìm file tồn tại trong danh sách đường dẫn truyền vào
        for (String path : fxmlPaths) {
            fxmlLocation = getClass().getResource(path);
            if (fxmlLocation != null) {
                chosenPath = path;
                break; // Tìm thấy file rồi thì dừng quét
            }
        }

        // Nếu tất cả các đường dẫn đều không tồn tại file FXML
        if (fxmlLocation == null) {
            System.err.println("❌ Không tìm thấy file FXML ở bất kỳ đường dẫn nào được cung cấp!");
            return null;
        }

        try {
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
            System.err.println("❌ Lỗi cấu trúc / cú pháp FXML bên trong file: " + chosenPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * HOÁN ĐỔI CLASS CSS CHUẨN
     */
    private void setButtonActive(Button activeButton) {
        Button[] buttons = {btnDashboard, btnUserMgmt};

        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
            }
        }

        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button");
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    @FXML
    private void openDashboard() {
        setButtonActive(btnDashboard);
        System.out.println("-> Mở giao diện Thống kê Tổng quan");

        // 🌟 ĐÃ SỬA: Nạp giao diện Dashboard lên màn hình (kèm đường dẫn dự phòng)
        loadAdminPage("/view/admin/AdminDashboardView.fxml", "/view/AdminDashboardView.fxml");
    }

    @FXML
    private void openUserMgmt() {
        setButtonActive(btnUserMgmt);
        System.out.println("-> Mở giao diện Quản lý thành viên");

        // 🌟 ĐÃ SỬA: Code siêu ngắn gọn nhờ tận dụng cơ chế quét đa đường dẫn mới
        loadAdminPage("/view/admin/AdminUserMgmtView.fxml", "/view/AdminUserMgmtView.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            // Hủy bỏ liên kết Singleton trước khi thoát để giải phóng RAM hoàn toàn
            instance = null;

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
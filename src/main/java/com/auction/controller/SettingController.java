package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class SettingController {

    // --- KHAI BÁO CÁC BIẾN UI (Phải khớp fx:id trong FXML) ---
    @FXML private VBox sidebar;
    @FXML private Button btnHome, btnProducts, btnCreate, btnHistory;

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;

    @FXML private PasswordField txtCurrentPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    private boolean isCollapsed = false;

    // --- 1. LOGIC ĐÓNG/MỞ SIDEBAR (Đồng bộ các trang) ---
    @FXML
    private void toggleSidebar() {
        if (isCollapsed) {
            sidebar.setMinWidth(250);
            sidebar.setPrefWidth(250);
            btnHome.setText("🏠  Trang chủ");
            btnProducts.setText("📦  Sản phẩm của tôi");
            btnCreate.setText("➕  Tạo phiên đấu giá");
            btnHistory.setText("🕘  Lịch sử");
            isCollapsed = false;
        } else {
            sidebar.setMinWidth(70);
            sidebar.setPrefWidth(70);
            btnHome.setText("🏠");
            btnProducts.setText("📦");
            btnCreate.setText("➕");
            btnHistory.setText("🕘");
            isCollapsed = true;
        }
    }

    // --- 2. XỬ LÝ LƯU THÔNG TIN CÁ NHÂN ---
    @FXML
    private void handleSaveInfo() {
        String name = txtFullName.getText();
        String email = txtEmail.getText();
        String phone = txtPhone.getText();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng không để trống Họ tên và Email!");
            return;
        }

        // Gọi Service cập nhật database tại đây
        System.out.println("Đang lưu: " + name + " - " + email);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin cá nhân!");
    }

    // --- 3. XỬ LÝ ĐỔI MẬT KHẨU ---
    @FXML
    private void handleChangePassword() {
        String current = txtCurrentPass.getText();
        String next = txtNewPass.getText();
        String confirm = txtConfirmPass.getText();

        if (current.isEmpty() || next.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ mật khẩu!");
            return;
        }

        if (!next.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu mới không khớp!");
            return;
        }

        // Logic kiểm tra mật khẩu cũ và lưu mật khẩu mới
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu đã được thay đổi!");

        // Xóa sạch các ô nhập sau khi thành công
        txtCurrentPass.clear();
        txtNewPass.clear();
        txtConfirmPass.clear();
    }

    // Hàm tiện ích hiển thị thông báo
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
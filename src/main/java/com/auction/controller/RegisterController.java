package com.auction.controller;

import com.auction.service.UserService; // Import Service mới
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtEmail; // Bạn nên thêm một ô nhập Email trong FXML nhé

    // Khai báo Service để dùng
    private UserService userService = new UserService();

    @FXML
    void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        // Vì Database của mình có cột Email lấy từ txtEmail hoặc để tạm một giá trị
        String email = (txtEmail != null) ? txtEmail.getText().trim() : username + "@auction.com";

        // 1. Kiểm tra cơ bản
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Lỗi", "Không được để trống thông tin!");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert(AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // 2. GỌI DATABASE THAY VÌ USERSTORE
        // Mình truyền 4 tham số: Username, Password, Email, và Role (mặc định là USER)
        boolean success = userService.register(username, password, email, "USER");

        if (success) {
            showAlert(AlertType.INFORMATION, "Thành công", "Đăng ký thành công vào Database!");
            goToLogin(event);
        } else {
            showAlert(AlertType.ERROR, "Thất bại", "Tên đăng nhập đã tồn tại hoặc lỗi kết nối Database!");
        }
    }

    // Các hàm goToLogin, switchScene, showAlert giữ nguyên như code cũ của bạn...
    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "/view/LoginView.fxml", "Đăng nhập");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package com.auction.controller;

import com.auction.model.UserStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;


public class RegisterController {


    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Chuyển về Đăng nhập (Giữ kích thước - Chống nhảy disco)
    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "/view/LoginView.fxml", "Đăng nhập");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Lỗi", "Không được để trống thông tin!");
            return;
        }
        if (UserStore.users.containsKey(username)) {
            showAlert(AlertType.WARNING, "Lỗi", "Tên đăng nhập đã tồn tại!");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Lưu vào bộ nhớ tạm
        UserStore.users.put(username, password);

        showAlert(AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Giờ bạn có thể đăng nhập.");
        goToLogin(event); // Đăng ký xong tự về trang đăng nhập
    }


    @FXML
    void handleDemoBuyer(ActionEvent event) {
        txtUsername.setText("buyer_demo");
        txtPassword.setText("123");
        txtConfirmPassword.setText("123");
    }

    @FXML
    void handleDemoSeller(ActionEvent event) {
        txtUsername.setText("seller_demo");
        txtPassword.setText("123");
        txtConfirmPassword.setText("123");
    }

    // Hàm dùng chung để chuyển cảnh không bị nhảy kích thước
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));

            // Lấy Stage từ sự kiện
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // THAY ĐỔI QUAN TRỌNG: Chỉ thay root của Scene hiện tại
            stage.getScene().setRoot(root);

            stage.setTitle(title);
            // Bạn không cần lưu width/height hay setWidth/Height nữa vì Stage không hề bị đổi Scene
        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlPath);
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
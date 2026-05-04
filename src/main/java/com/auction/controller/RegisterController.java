package com.auction.controller;

import com.auction.model.UserStore; // Nhớ import cái store ở bước 1
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Lấy kích thước thực tế hiện tại của cửa sổ (bao gồm cả viền)
            double width = stage.getWidth();
            double height = stage.getHeight();

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));

            // Ép Scene mới nhận luôn kích thước cũ
            Scene scene = new Scene(root, width, height);

            stage.setScene(scene);
            stage.setTitle(title);

            // Đảm bảo Stage không bị co lại sau khi setScene
            stage.setWidth(width);
            stage.setHeight(height);
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
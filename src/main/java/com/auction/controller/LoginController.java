package com.auction.controller;

import com.auction.model.UserStore;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    void handleLogin(ActionEvent event) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (UserStore.users.containsKey(user) && UserStore.users.get(user).equals(pass)) {
            switchScene(event, "/view/MainView.fxml", "Hệ thống Đấu giá");
        } else {
            showError("Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    void handleDemoBuyer(ActionEvent event) {
        usernameField.setText("buyer");
        passwordField.setText("123");
        handleLogin(event);
    }

    @FXML
    void handleDemoSeller(ActionEvent event) {
        usernameField.setText("seller");
        passwordField.setText("123");
        handleLogin(event);
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản");
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }

    /**
     * Hàm switchScene tối ưu: Đảm bảo căn giữa tuyệt đối ở mọi lần chuyển
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            // 1. Lấy Stage hiện tại
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 2. Load nội dung mới (Root mới)
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // 3. Thay đổi nội dung của Scene ĐANG CÓ thay vì tạo Scene mới
            // Việc này giữ nguyên mọi trạng thái cửa sổ, không bao giờ bị nhảy
            stage.getScene().setRoot(root);

            // 4. Cập nhật tiêu đề
            stage.setTitle(title);

            // 5. Ép JavaFX tính toán lại layout ngay lập tức để căn giữa
            root.requestLayout();

        } catch (IOException e) {
            System.err.println("Lỗi load file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}

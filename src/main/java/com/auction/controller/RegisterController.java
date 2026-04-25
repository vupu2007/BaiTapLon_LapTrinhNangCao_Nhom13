package com.auction.controller;

import com.auction.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label regErrorLabel;

    private UserService userService = new UserService();

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = regUsernameField.getText();
        String password = regPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        // chỉ giữ validate UI cơ bản
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || role == null) {
            showError("Vui lòng điền đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        // gọi service
        boolean success = userService.register(username, password, role);

        if (success) {
            showSuccess("Đăng ký thành công!");
        } else {
            showError("Đăng ký thất bại!");
        }
    }

    private void showError(String msg) {
        regErrorLabel.setText(msg);
        regErrorLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String msg) {
        regErrorLabel.setText(msg);
        regErrorLabel.setStyle("-fx-text-fill: green;");
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        System.out.println("Quay lại màn hình Đăng nhập...");
    }
}
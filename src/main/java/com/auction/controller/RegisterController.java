package com.auction.controller;

import com.auction.service.AccountService; // Import Service mới
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    // Khai báo Service để dùng
    private AccountService accountService = new AccountService();

    @FXML
    void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

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
        // Mình truyền 3 tham số: Username, Password, và Role (mặc định là USER)
        boolean success = accountService.register(username, password, "USER");

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
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ToggleButton togglePasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ToggleButton toggleConfirmPasswordBtn;

    @FXML
    public void initialize() {
        // Đồng bộ nội dung giữa 2 ô Mật khẩu
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        // Đồng bộ nội dung giữa 2 ô Xác nhận mật khẩu
        confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

    @FXML
    private void togglePasswordVisibility() {
        if (togglePasswordBtn.isSelected()) {
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            togglePasswordBtn.setText("👁‍🗨");
        } else {
            passwordTextField.setVisible(false);
            passwordField.setVisible(true);
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        if (toggleConfirmPasswordBtn.isSelected()) {
            confirmPasswordTextField.setVisible(true);
            confirmPasswordField.setVisible(false);
            toggleConfirmPasswordBtn.setText("👁‍🗨");
        } else {
            confirmPasswordTextField.setVisible(false);
            confirmPasswordField.setVisible(true);
            toggleConfirmPasswordBtn.setText("👁");
        }
    }

}
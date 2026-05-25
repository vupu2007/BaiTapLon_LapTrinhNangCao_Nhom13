package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.shared.network.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ToggleButton togglePasswordBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ToggleButton toggleConfirmPasswordBtn;
    @FXML private TextField emailField;

    // CHUẨN ENTERPRISE: Khởi tạo Service nghiệp vụ riêng tại Client
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        confirmPasswordTextField.setVisible(false);
        confirmPasswordTextField.setManaged(false);
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String email = emailField.getText().trim();

        // 1. Kiểm tra dữ liệu trực tiếp tại Client để tiết kiệm băng thông đường truyền
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi form", "Vui lòng nhập đầy đủ tất cả các trường dữ liệu!");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp! Vui lòng kiểm tra lại.");
            return;
        }

        // 2. Giao phó toàn bộ tác vụ mạng cho lớp Service lo chạy ngầm (Background Thread)
        authService.registerAsync(username, password, email, response -> {

            // 3. Nhận kết quả sạch trả về từ luồng mạng và xử lý thông báo lên UI công khai
            if (response != null && response.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Đăng ký thành công", response.getMessage());
                goToLogin(event);
            } else {
                String errorMsg = (response != null) ? response.getMessage() : "Mất kết nối với máy chủ.";
                showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", errorMsg);
            }
        });
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "/view/LoginView.fxml", "Đăng nhập");
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean show = togglePasswordBtn.isSelected();

        passwordTextField.setVisible(show);
        passwordTextField.setManaged(show);

        passwordField.setVisible(!show);
        passwordField.setManaged(!show);

        togglePasswordBtn.setText(show ? "👁‍🗨" : "👁");
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        boolean show = toggleConfirmPasswordBtn.isSelected();

        confirmPasswordTextField.setVisible(show);
        confirmPasswordTextField.setManaged(show);

        confirmPasswordField.setVisible(!show);
        confirmPasswordField.setManaged(!show);

        toggleConfirmPasswordBtn.setText(show ? "👁‍🗨" : "👁");
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
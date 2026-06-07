package com.auction.client.controller;

import com.auction.client.service.AccountService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;

    private final AccountService accountService = new AccountService();

    @FXML
    public void handleSendOTP() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();

        if (username.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu",
                    "Vui lòng nhập đầy đủ Username và Email!");
            return;
        }

        txtUsername.setDisable(true);
        txtEmail.setDisable(true);

        // ✅ Controller không biết gì về Request/MessageType
        accountService.forgotPasswordAsync(username, email, response -> {
            txtUsername.setDisable(false);
            txtEmail.setDisable(false);

            if (response != null && response.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "OTP đã được gửi tới email của bạn!");
            } else {
                String msg = response != null ? response.getMessage() : "Lỗi kết nối Server!";
                showAlert(Alert.AlertType.ERROR, "Thất bại", msg);
            }
        });
    }

    @FXML
    public void handleBackToLogin() {
        com.auction.client.MainApp.changeScene("/view/LoginView.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
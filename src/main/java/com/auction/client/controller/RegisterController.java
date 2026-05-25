package com.auction.client.controller;

import com.auction.shared.network.Response;
import javafx.application.Platform;
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

    // 🌟 ĐỒNG BỘ KIẾN TRÚC: Tái sử dụng AccountController đã chuẩn hóa ở tầng Client
    private final AccountController accountController = new AccountController();

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

        // 1. Kiểm tra dữ liệu trực tiếp tại Client
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi form", "Vui lòng nhập đầy đủ tất cả các trường dữ liệu!");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp! Vui lòng kiểm tra lại.");
            return;
        }

        // 🚀 2. TÁCH THREAD CHẠY NGẦM: Đưa tác vụ mạng xuống luồng riêng giống hệt LoginController
        Thread registerWorker = new Thread(() -> {
            try {
                // Gọi xử lý qua AccountController thay vì gọi Socket lẻ tẻ
                Response response = accountController.registerUser(username, password, email);

                // 🌟 3. ĐƯA VỀ LUỒNG UI: Đảm bảo hiển thị popup và chuyển cảnh không bị crash
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Đăng ký thành công", "Tài khoản của bạn đã được tạo thành công!");
                        goToLogin(event);
                    } else {
                        String errorMsg = (response != null) ? response.getMessage() : "Đăng ký thất bại.";
                        showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", errorMsg);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đã xảy ra sự cố mạng ngầm: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }, "RegisterNetworkWorkerThread");

        registerWorker.setDaemon(true);
        registerWorker.start();
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
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("Không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // 🌟 TỰ ĐỘNG BẢO VỆ THREAD: Đảm bảo Alert luôn hiển thị an toàn dù gọi ở bất kỳ đâu
    private void showAlert(Alert.AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
}
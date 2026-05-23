package com.auction.client.controller;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;
import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Account;
import com.auction.client.util.CurrentAccount;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Áp dụng liên kết 2 chiều giống RegisterController để mật khẩu luôn đồng bộ
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = (usernameField.getText() != null) ? usernameField.getText().trim() : "";
        // Vì đã bindBidirectional nên lấy thẳng từ passwordField là luôn chính xác
        String password = (passwordField.getText() != null) ? passwordField.getText() : "";

        // 1. Validate cơ bản ở Client
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // 2. LUỒNG CHẠY THỰC TẾ QUA SERVER BẰNG SOCKET (ĐÃ BỎ BYPASS LỖI)
        String[] loginData = {username, password};
        Request request = new Request(MessageType.LOGIN, loginData);

        try {
            // Gửi Request và nhận Response từ Server thông qua kết nối duy nhất
            Response response = ClientSocket.getInstance().sendRequest(request);

            if (response != null && response.isSuccess()) {
                // Ép kiểu dữ liệu Server trả về thành đối tượng Account (Do DB cấp)
                Account loggedIn = (Account) response.getData();
                CurrentAccount.setAccount(loggedIn);

                System.out.println("-> Đăng nhập thành công! Username: " + loggedIn.getUsername() + " | Role: " + loggedIn.getRole());

                // Phân quyền chuyển màn hình dựa trên Role thực tế trả về từ DB
                if ("ADMIN".equals(loggedIn.getRole())) {
                    System.out.println("🚀 Đang chuyển hướng sang giao diện AdminLayoutView...");
                    switchScene(event, "/view/AdminLayoutView.fxml", "Quản trị hệ thống");
                } else {
                    System.out.println("🛒 Đang chuyển hướng sang giao diện khách hàng MainLayout...");
                    switchScene(event, "/view/MainLayout.fxml", "Hệ thống đấu giá");
                }
            } else {
                String errorMsg = (response != null) ? response.getMessage() : "Sai tài khoản hoặc mật khẩu!";
                showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", errorMsg);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ Server. Hãy chắc chắn Server đang bật!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản");
    }

    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
        isPasswordVisible = !isPasswordVisible;

        visiblePasswordField.setVisible(isPasswordVisible);
        visiblePasswordField.setManaged(isPasswordVisible);

        passwordField.setVisible(!isPasswordVisible);
        passwordField.setManaged(!isPasswordVisible);
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Không tìm thấy hoặc lỗi file FXML: " + fxmlPath);
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
package com.auction.client.controller;

import com.auction.server.service.AccountService;
import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
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
    @FXML private TextField visiblePasswordField; // Giữ nguyên trường hiện password của bạn

    private final AccountService accountService = new AccountService();
    private boolean isPasswordVisible = false; // Giữ nguyên trạng thái ẩn/hiện mật khẩu

    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();

        // SỬA ĐỂ KHÔNG BỊ LỖI KHI ĐANG HIỆN MẬT KHẨU:
        // Nếu mật khẩu đang hiện thì lấy ở ô visible, nếu đang ẩn thì lấy ở ô password
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();

        // MẸO KHẨN CẤP ĐỂ TEAM TEST:
        // Nếu Server/DB chưa xử lý kịp class Admin, chỉ cần gõ đúng username là "admin" sẽ bốc thẳng vào trang Admin luôn
        if ("admin".equals(username)) {
            switchScene(event, "/view/AdminLayoutView.fxml", "Quản trị hệ thống - Admin Area");
            return;
        }

        // LUỒNG CHẠY THỰC TẾ QUA SERVER:
        Account loggedIn = accountService.login(username, password);
        if (loggedIn != null) {
            CurrentAccount.setAccount(loggedIn);

            // IN LOG RA ĐỂ TEAM THEO DÕI TRÊN CONSOLE CHO DỄ
            System.out.println("-> Đăng nhập thành công! Username: " + loggedIn.getUsername() + " | Role: " + loggedIn.getRole());

            // THAY instanceof BẰNG KIỂM TRA CHUỖI ROLE ĐỂ KHỚP VỚI DATABASE
            if ("ADMIN".equals(loggedIn.getRole())) {
                System.out.println("🚀 Đang chuyển hướng sang giao diện AdminLayoutView...");
                switchScene(event, "/view/AdminLayoutView.fxml", "Quản trị hệ thống");
            } else {
                System.out.println("🛒 Đang chuyển hướng sang giao diện khách hàng MainLayout...");
                switchScene(event, "/view/MainLayout.fxml", "Hệ thống đấu giá");
            }

        } else {
            showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", "Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.centerOnScreen(); // Giúp giao diện Admin to ra sẽ tự căn giữa màn hình
        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlPath);
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

    //  LOGIC ẨN / HIỆN PASSWORD
    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
        if (isPasswordVisible) {
            // Chuyển từ hiện → ẩn
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        } else {
            // Chuyển từ ẩn → hiện
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);
        }
        isPasswordVisible = !isPasswordVisible;
    }
}
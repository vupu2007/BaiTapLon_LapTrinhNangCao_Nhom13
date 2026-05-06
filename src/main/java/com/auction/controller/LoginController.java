package com.auction.controller;

import com.auction.model.User; // Import model User
import com.auction.model.Admin; // Import model Admin
import com.auction.service.UserService; // Import Service
import com.auction.util.CurrentUser; // Class để lưu phiên đăng nhập
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    private final String API_URL = "http://localhost:8080/api/auth/login";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // Khai báo Service để kết nối Database
    private UserService userService = new UserService();

    @FXML
    void handleLogin(ActionEvent event) {
        String userStr = usernameField.getText().trim();
        String passStr = passwordField.getText();

        // 1. Gọi Database để kiểm tra
        User loggedInUser = userService.login(userStr, passStr);

        if (loggedInUser != null) {

            // Cất ngay khi biết loggedInUser không bị null
            CurrentUser.setUser(loggedInUser);
             System.out.println("Đã cất user " + loggedInUser.getUsername() + " vào phiên làm việc!");
            // ----------------------------------

            // 2. Sau khi cất xong mới bắt đầu chuyển trang
            if (loggedInUser instanceof Admin) {
                switchScene(event, "/view/AdminDashboard.fxml", "Quản trị hệ thống");
            } else {
                switchScene(event, "/view/MainAuctionView.fxml", "Hệ thống Đấu giá");
            }
        } else {
            // Nếu login thất bại thì không cất gì cả
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sai tài khoản hoặc mật khẩu!");
            alert.show();
        }
    }

    @FXML
    void handleDemoBuyer(ActionEvent event) {
        // Jeff nhớ đăng ký user này trước trong Database thì bấm mới được nhé
        usernameField.setText("buyer_demo");
        passwordField.setText("123");
        handleLogin(event);
    }

    @FXML
    void handleDemoSeller(ActionEvent event) {
        usernameField.setText("seller_demo");
        passwordField.setText("123");
        handleLogin(event);
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Dùng setRoot để giữ kích thước Stage không bị nhảy
            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Admin;
import com.auction.service.UserService;
import com.auction.util.CurrentAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private UserService userService = new UserService();

    @FXML
    void handleLogin(ActionEvent event) {
        String userStr = usernameField.getText().trim();
        String passStr = passwordField.getText();

        // Sửa kiểu dữ liệu từ User sang Account
        Account loggedInAccount = userService.login(userStr, passStr);

        if (loggedInAccount != null) {
            CurrentAccount.setUser(loggedInAccount);
            System.out.println("Đã cất user " + loggedInAccount.getUsername() + " vào phiên làm việc!");

            if (loggedInAccount instanceof Admin) {
                switchScene(event, "/view/AdminDashboard.fxml", "Quản trị hệ thống");
            } else {
                switchScene(event, "/view/MainAuctionView.fxml", "Hệ thống Đấu giá");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sai tài khoản hoặc mật khẩu!");
            alert.show();
        }
    }

    @FXML
    void handleDemoBuyer(ActionEvent event) {
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
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;
    @FXML private Button btnLogin;

    @FXML
    public void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.equals("admin") && password.equals("123456")) {
            lblMessage.setStyle("-fx-text-fill: green;");
            lblMessage.setText("Đăng nhập thành công!");
        } else {
            lblMessage.setStyle("-fx-text-fill: red;");
            lblMessage.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }
}
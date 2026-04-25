package com.auction.controller;

import com.auction.model.User;
import com.auction.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;

    // thêm service
    private UserService userService = new UserService();

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // gọi service thay vì tự xử lý
        User user = userService.login(username, password);

        if (user != null) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Đăng nhập thành công!");

            System.out.println("User đăng nhập: " + user.getUsername());

            // TODO: chuyển sang màn hình chính
        } else {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        System.out.println("Đang chuyển sang màn hình Đăng ký...");
    }
}
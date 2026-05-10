package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Admin;
import com.auction.service.AccountService;
import com.auction.util.CurrentAccount;
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

    private final AccountService accountService = new AccountService();

    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        Account loggedIn = accountService.login(username, password);

        if (loggedIn != null) {
            CurrentAccount.setAccount(loggedIn);

            if (loggedIn instanceof Admin) {
                switchScene(event, "/view/AdminView.fxml", "Quản trị hệ thống");
            } else {
                switchScene(event, "/view/MainView.fxml", "Hệ thống đấu giá");
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
    // TextField để hiện password
    @FXML private TextField visiblePasswordField;
    // Trạng thái hiện / ẩn password
    private boolean isPasswordVisible = false;
    @FXML
    private void togglePasswordVisibility(ActionEvent event) {

        if (isPasswordVisible) {

            // Chuyển từ hiện → ẩn
            passwordField.setText(
                    visiblePasswordField.getText()
            );
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        } else {

            // Chuyển từ ẩn → hiện
            visiblePasswordField.setText(
                    passwordField.getText()
            );
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);
        }
        isPasswordVisible = !isPasswordVisible;
    }

}
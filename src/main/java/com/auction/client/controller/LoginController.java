package com.auction.client.controller;

import com.auction.client.service.AccountService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Account;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private ToggleButton togglePasswordBtn;

    private final AccountService accountService = new AccountService();
    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        switchScene(event, "/view/ForgotPasswordView.fxml", "Khôi phục mật khẩu - Đấu giá UET", false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = (usernameField.getText() != null) ? usernameField.getText().trim() : "";
        String password = (passwordField.getText() != null) ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu",
                    "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // ✅ Controller không biết gì về Request/MessageType
        accountService.loginUserAsync(username, password, response -> {
            if (response != null && response.isSuccess()) {
                Account loggedIn = (Account) response.getData();
                CurrentAccount.setAccount(loggedIn);
                if ("ADMIN".equals(loggedIn.getRole())) {
                    switchScene(event, "/view/MainLayout.fxml",
                            "Hệ thống đấu giá (Quyền: Admin)", true);
                } else {
                    switchScene(event, "/view/MainLayout.fxml",
                            "Hệ thống đấu giá", false);
                }
            } else {
                String errorMsg = response != null ? response.getMessage() : "Đăng nhập thất bại!";
                showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", errorMsg);
            }
        });
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản", false);
    }

    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
        isPasswordVisible = !isPasswordVisible;
        visiblePasswordField.setVisible(isPasswordVisible);
        visiblePasswordField.setManaged(isPasswordVisible);
        passwordField.setVisible(!isPasswordVisible);
        passwordField.setManaged(!isPasswordVisible);
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title, boolean isAdmin) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Không tìm thấy file FXML: " + fxmlPath); return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            if (isAdmin && fxmlPath.contains("MainLayout")) {
                Object controller = loader.getController();
                if (controller instanceof MainLayoutController) {
                    ((MainLayoutController) controller).setAdminMode(true);
                }
            }
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> showAlert(type, title, message));
        }
    }
}
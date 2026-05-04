package com.auction.controller;

import com.auction.model.UserStore;
import com.auction.service.LoginService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // THÊM SERVICE
    private final LoginService loginService = new LoginService();

    @FXML
    void handleLogin(ActionEvent event) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        // GỌI SERVICE
        loginService.login(user, pass);

        if (UserStore.users.containsKey(user) && UserStore.users.get(user).equals(pass)) {
            System.out.println("Đăng nhập thành công!");
            switchScene(event, "/view/MainAuctionView.fxml", "Hệ thống Đấu giá");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sai tài khoản hoặc mật khẩu!");
            alert.show();
        }
    }

    @FXML
    void handleDemoBuyer(ActionEvent event) {
        usernameField.setText("buyer");
        passwordField.setText("123");
        handleLogin(event);
    }

    @FXML
    void handleDemoSeller(ActionEvent event) {
        usernameField.setText("seller");
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
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        boolean isAuthenticated = true;

        if (isAuthenticated) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MainView.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) loginButton.getScene().getWindow();

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Hệ thống đấu giá - Dashboard");
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Không thể tải màn hình chính!");
            }
        } else {
        }
    }
}
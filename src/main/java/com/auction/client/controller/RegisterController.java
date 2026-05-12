package com.auction.client.controller;

import com.auction.server.service.AccountService;
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

    private final AccountService accountService = new AccountService();

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

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Lỗi",
                    "Không được để trống thông tin!");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR,
                    "Lỗi",
                    "Mật khẩu xác nhận không khớp!");
            return;
        }
        String email = emailField.getText().trim();

        boolean success =
                accountService.register(
                        username,
                        password,
                        email
                );

        if (success) {

            showAlert(Alert.AlertType.INFORMATION,
                    "Thành công",
                    "Đăng ký thành công!");

            goToLogin(event);
        } else {

            showAlert(Alert.AlertType.ERROR,
                    "Thất bại",
                    "Tên đăng nhập đã tồn tại hoặc lỗi database!");
        }
    }
    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event,
                "/view/LoginView.fxml",
                "Đăng nhập");
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

        boolean show =
                toggleConfirmPasswordBtn.isSelected();

        confirmPasswordTextField.setVisible(show);
        confirmPasswordTextField.setManaged(show);

        confirmPasswordField.setVisible(!show);
        confirmPasswordField.setManaged(!show);

        toggleConfirmPasswordBtn.setText(show ? "👁‍🗨" : "👁");
    }

    private void switchScene(ActionEvent event,
                             String fxmlPath,
                             String title) {
        try {

            Parent root =
                    FXMLLoader.load(
                            getClass().getResource(fxmlPath));

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showAlert(Alert.AlertType type,
                           String title,
                           String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
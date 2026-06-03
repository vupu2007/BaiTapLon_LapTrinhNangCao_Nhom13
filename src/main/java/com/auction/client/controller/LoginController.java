package com.auction.client.controller;

import com.auction.shared.network.Response;
import com.auction.shared.model.Account;
import com.auction.client.util.CurrentAccount;
import javafx.util.Pair; // Dùng cho Pair
import javafx.scene.layout.GridPane; // Dùng cho GridPane
import javafx.scene.control.Label; // Dùng cho Label
import javafx.scene.control.TextField; // Dùng cho TextField
import javafx.scene.control.Dialog; // Dùng cho Dialog
import javafx.scene.control.ButtonType; // Dùng cho ButtonType
import javafx.scene.control.ButtonBar; // Dùng cho ButtonBar

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*; // Import thêm để dùng Dialog
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Optional; // Import để xử lý kết quả Dialog

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private ToggleButton togglePasswordBtn;

    private final AccountController accountController = new AccountController();
    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
    }

    // --- TÍNH NĂNG MỚI: Xử lý Quên mật khẩu ---
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // Tạo dialog để lấy thông tin
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Khôi phục mật khẩu");
        dialog.setHeaderText("Nhập thông tin tài khoản");

        ButtonType sendButtonType = new ButtonType("Gửi yêu cầu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField username = new TextField(); username.setPromptText("Username");
        TextField email = new TextField(); email.setPromptText("Email");
        grid.add(new Label("Username:"), 0, 0); grid.add(username, 1, 0);
        grid.add(new Label("Email:"), 0, 1); grid.add(email, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) return new Pair<>(username.getText(), email.getText());
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            // Gọi hàm accountController.forgotPassword mới của bạn
            Thread thread = new Thread(() -> {
                Response response = accountController.forgotPassword(pair.getKey(), pair.getValue());

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", response.getMessage());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
                    }
                });
            });
            thread.setDaemon(true);
            thread.start();
        });
    }
    @FXML
    void handleLogin(ActionEvent event) {
        String username = (usernameField.getText() != null) ? usernameField.getText().trim() : "";
        String password = (passwordField.getText() != null) ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        Thread loginWorker = new Thread(() -> {
            try {
                Response response = accountController.loginUser(username, password);
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        Account loggedIn = (Account) response.getData();
                        CurrentAccount.setAccount(loggedIn);
                        if ("ADMIN".equals(loggedIn.getRole())) {
                            switchScene(event, "/view/MainLayout.fxml", "Hệ thống đấu giá (Quyền: Admin)", true);
                        } else {
                            switchScene(event, "/view/MainLayout.fxml", "Hệ thống đấu giá", false);
                        }
                    } else {
                        String errorMsg = (response != null) ? response.getMessage() : "Đăng nhập thất bại!";
                        showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", errorMsg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Sự cố: " + e.getMessage()));
                e.printStackTrace();
            }
        }, "LoginNetworkWorkerThread");
        loginWorker.setDaemon(true);
        loginWorker.start();
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
            if (fxmlUrl == null) return;
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
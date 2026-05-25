package com.auction.client.controller;

import com.auction.shared.network.Response;
import com.auction.shared.model.Account;
import com.auction.client.util.CurrentAccount;

import javafx.application.Platform;
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
    @FXML private TextField visiblePasswordField;

    // 🌟 KHỞI TẠO TẦNG CONTROLLER: Để xử lý logic mạng thay vì gọi Socket trực tiếp
    private final AccountController accountController = new AccountController();
    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Áp dụng liên kết 2 chiều của bạn - Giữ nguyên
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = (usernameField.getText() != null) ? usernameField.getText().trim() : "";
        String password = (passwordField.getText() != null) ? passwordField.getText() : "";

        // 1. Validate cơ bản ở Client - Giữ nguyên
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // 🚀 Tách một Thread chạy ngầm để gửi lệnh đăng nhập qua AccountController
        Thread loginWorker = new Thread(() -> {
            try {
                // 🌟 THAY ĐỔI CỐT LÕI: Giao diện chỉ ra lệnh, việc kết nối cứ để AccountController lo
                Response response = accountController.loginUser(username, password);

                // Nhận phản hồi xong -> Đẩy logic xử lý giao diện về lại luồng JavaFX UI an toàn
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        Account loggedIn = (Account) response.getData();
                        CurrentAccount.setAccount(loggedIn);

                        System.out.println("-> Đăng nhập thành công! Username: " + loggedIn.getUsername() + " | Role: " + loggedIn.getRole());

                        // Phân quyền chuyển màn hình
                        if ("ADMIN".equals(loggedIn.getRole())) {
                            System.out.println("🚀 Đang chuyển hướng sang giao diện AdminLayoutView...");
                            switchScene(event, "/view/AdminLayoutView.fxml", "Quản trị hệ thống");
                        } else {
                            System.out.println("🛒 Đang chuyển hướng sang giao diện khách hàng MainLayout...");
                            switchScene(event, "/view/MainLayout.fxml", "Hệ thống đấu giá");
                        }
                    } else {
                        // Lấy chuẩn câu báo lỗi từ Server dội về (Ví dụ: "Mật khẩu không đúng!")
                        String errorMsg = (response != null) ? response.getMessage() : "Đăng nhập thất bại!";
                        showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", errorMsg);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đã xảy ra sự cố ngoài ý muốn: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }, "LoginNetworkWorkerThread");

        loginWorker.setDaemon(true);
        loginWorker.start();
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản");
    }

    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
        isPasswordVisible = !isPasswordVisible;

        visiblePasswordField.setVisible(isPasswordVisible);
        visiblePasswordField.setManaged(isPasswordVisible);

        passwordField.setVisible(!isPasswordVisible);
        passwordField.setManaged(!isPasswordVisible);
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Không tìm thấy hoặc lỗi file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Tự động điều phối hiển thị popup an toàn dù gọi từ bất kỳ luồng nào
    private void showAlert(Alert.AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
}
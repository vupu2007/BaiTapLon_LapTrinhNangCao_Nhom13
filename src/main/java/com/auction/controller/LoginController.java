package com.auction.controller;

import com.auction.model.UserStore;
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

    // Đã đổi tên để khớp 100% với file FXML của bạn
    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel; // Nếu FXML chưa có, bạn có thể xóa dòng này hoặc thêm vào FXML

    @FXML
    void handleLogin(ActionEvent event) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        // Kiểm tra đăng nhập dùng UserStore (đã đăng ký trước đó)
        if (UserStore.users.containsKey(user) && UserStore.users.get(user).equals(pass)) {
            System.out.println("Đăng nhập thành công!");
            switchScene(event, "/view/MainAuctionView.fxml", "Hệ thống Đấu giá");
        } else {
            // Hiển thị thông báo lỗi
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sai tài khoản hoặc mật khẩu!");
            alert.show();
        }
    }

    @FXML
    void handleDemoBuyer(ActionEvent event) {
        usernameField.setText("buyer");
        passwordField.setText("123");
        handleLogin(event); // Bấm hộ người dùng luôn
    }

    @FXML
    void handleDemoSeller(ActionEvent event) {
        usernameField.setText("seller");
        passwordField.setText("123");
        handleLogin(event); // Bấm hộ người dùng luôn
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/view/RegisterView.fxml", "Đăng ký tài khoản");
    }

    // Hàm dùng chung để chuyển trang
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));

            // Lấy Stage hiện tại
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // THAY ĐỔI NÀY: Thay vì tạo Scene mới, ta lấy Scene hiện tại và đổi Root
            stage.getScene().setRoot(root);

            stage.setTitle(title);
            // Không cần setWidth/Height thủ công nữa vì Stage không bị đổi Scene nên không nhảy size
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleLogin() {
        // Giả sử bạn đã kiểm tra tài khoản/mật khẩu thông qua UserService
        boolean isAuthenticated = true; // Kết quả trả về từ logic đăng nhập

        if (isAuthenticated) {
            try {
                // 1. Tải file FXML của màn hình chính
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MainView.fxml"));
                Parent root = loader.load();

                // 2. Lấy Stage (cửa sổ) hiện tại từ một node bất kỳ trên giao diện (ví dụ: loginButton)
                Stage stage = (Stage) loginButton.getScene().getWindow();

                // 3. Thiết lập Scene mới với MainView
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Hệ thống đấu giá - Dashboard");
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Không thể tải màn hình chính!");
            }
        } else {
            // Hiển thị thông báo lỗi nếu đăng nhập thất bại
        }
    }
    @FXML
    private void togglePassword() {
        // code ở đây
    }

}
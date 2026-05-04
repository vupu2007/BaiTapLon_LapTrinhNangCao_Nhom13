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

    // Hàm dùng chung để chuyển trang KHÔNG NHẢY DISCO
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Lưu lại kích thước hiện tại trước khi đổi
            double width = stage.getWidth();
            double height = stage.getHeight();

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Áp lại kích thước cũ ngay lập tức
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setTitle(title);

        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
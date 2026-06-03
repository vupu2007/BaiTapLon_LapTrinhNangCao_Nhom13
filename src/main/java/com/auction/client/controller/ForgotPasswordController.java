package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import com.auction.shared.network.Request;
import com.auction.shared.network.MessageType;

public class ForgotPasswordController {
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;

    @FXML
    public void handleSendOTP() {
        System.out.println("DEBUG: Hàm handleSendOTP đã được gọi!");

        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();

        if (username.isEmpty() || email.isEmpty()) {
            System.out.println("DEBUG: Username hoặc Email bị trống!");
            return;
        }

        // Đóng gói request quên mật khẩu
        Request req = new Request(MessageType.FORGOT_PASSWORD, new String[]{username, email});

        System.out.println("DEBUG: Đang chuẩn bị tạo Thread ngầm để gửi request FORGOT_PASSWORD...");

        // 🌟 TỐI ƯU: Tạo Thread độc lập để xử lý tác vụ mạng, giữ cho UI luôn mượt mà
        Thread networkWorker = new Thread(() -> {
            System.out.println("DEBUG: [Thread Ngầm] Đang gửi request qua ClientSocket...");

            // Hàm sendRequest chạy ở đây có block bao lâu thì màn hình bên ngoài vẫn bấm click bình thường
            ClientSocket.getInstance().sendRequest(req);

            System.out.println("DEBUG: [Thread Ngầm] Đã xử lý xong request FORGOT_PASSWORD!");
        }, "ForgotPassword-Network-Worker");

        // Đặt làm Daemon Thread để tự động tắt khi đóng ứng dụng
        networkWorker.setDaemon(true);
        networkWorker.start();
    }

    @FXML
    public void handleBackToLogin() {
        System.out.println("DEBUG: Đang quay lại màn hình Login...");
        com.auction.client.MainApp.changeScene("/view/LoginView.fxml");
    }
}
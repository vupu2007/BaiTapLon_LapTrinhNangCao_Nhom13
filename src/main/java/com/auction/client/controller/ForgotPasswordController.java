package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import com.auction.shared.network.Request;
import com.auction.shared.network.MessageType;
import javafx.scene.control.Alert;
import javafx.application.Platform;

public class ForgotPasswordController {
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;

    @FXML
    public void handleSendOTP() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();

        if (username.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Username và Email!");
            return;
        }

        // Hiển thị trạng thái đang gửi
        txtUsername.setDisable(true);
        txtEmail.setDisable(true);

        Thread networkWorker = new Thread(() -> {
            // Gửi request và nhận response
            Request req = new Request(MessageType.FORGOT_PASSWORD, new String[]{username, email});
            var response = ClientSocket.getInstance().sendRequest(req); // Giả sử hàm này trả về Response

            // Cập nhật kết quả lên UI
            Platform.runLater(() -> {
                txtUsername.setDisable(false);
                txtEmail.setDisable(false);

                if (response != null && response.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "OTP đã được gửi tới email của bạn!");
                } else {
                    String msg = (response != null) ? response.getMessage() : "Lỗi kết nối Server!";
                    showAlert(Alert.AlertType.ERROR, "Thất bại", msg);
                }
            });
        });
        networkWorker.setDaemon(true);
        networkWorker.start();
    }

    @FXML
    public void handleBackToLogin() {
        System.out.println("DEBUG: Đang quay lại màn hình Login...");
        com.auction.client.MainApp.changeScene("/view/LoginView.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        // Nếu đang ở trong luồng mạng (Thread), dùng Platform.runLater
        // Nếu ở trong luồng UI chính, gọi trực tiếp cũng được
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
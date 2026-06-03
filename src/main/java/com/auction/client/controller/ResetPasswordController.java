package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ResetPasswordController {

    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtOtp;
    @FXML
    private PasswordField txtNewPassword;

    @FXML
    public void handleResetPassword() {
        String username = txtUsername.getText().trim();
        String otp = txtOtp.getText().trim();
        String newPass = txtNewPassword.getText().trim();

        // Kiểm tra dữ liệu đầu vào
        if (username.isEmpty()) {
            showError("Lỗi", "Vui lòng nhập tên tài khoản!");
            return;
        }
        if (otp.length() != 6 || newPass.length() < 6) {
            showError("Lỗi", "OTP phải đúng 6 số, mật khẩu mới tối thiểu 6 ký tự!");
            return;
        }

        // Đóng gói request gửi lên Server
        Request req = new Request(MessageType.RESET_PASSWORD, new String[]{username, otp, newPass});

        // Gửi dữ liệu đồng bộ qua ClientSocket
        ClientSocket socket = ClientSocket.getInstance();
        Response res = socket.sendRequest(req);

        if (res != null && res.isSuccess()) {
            showInfo("Thành công", "Mật khẩu đã được cập nhật thành công!");

            // 🛠️ ĐÃ SỬA: Quay về màn hình Login chứ không dùng .hide() làm tắt app
            MainApp.changeScene("/view/LoginView.fxml");

        } else {
            showError("Lỗi", res != null ? res.getMessage() : "Không nhận được phản hồi từ server!");
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Để thông báo nhìn gọn gàng hơn
        alert.setContentText(content);
        alert.showAndWait(); // Khuyên dùng showAndWait để user bấm OK xong mới chuyển scene
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
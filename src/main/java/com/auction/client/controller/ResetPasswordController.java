package com.auction.client.controller;

import com.auction.client.ClientConnection;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import com.auction.shared.network.Request;
import com.auction.shared.network.MessageType;

public class ResetPasswordController {
    @FXML
    private TextField txtOtp;
    @FXML
    private PasswordField txtNewPassword;
    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    @FXML
    public void handleResetPassword() {
        String otp = txtOtp.getText().trim();
        String newPass = txtNewPassword.getText().trim();

        if (otp.length() != 6 || newPass.length() < 6) {
            showError("Lỗi", "OTP phải 6 số, mật khẩu mới tối thiểu 6 ký tự!");
            return;
        }

        // Gửi request lên Server và NHẬN PHẢN HỒI (nhờ hàm sendRequest trong ClientSocket)
        Request req = new Request(MessageType.RESET_PASSWORD, new String[]{username, otp, newPass});

        // Giả sử ClientSocket.getInstance() là class bạn dùng (vì tôi thấy bạn dùng ClientConnection, hãy đổi thành ClientSocket nếu cần)
        com.auction.client.network.ClientSocket socket = com.auction.client.network.ClientSocket.getInstance();
        com.auction.shared.network.Response res = socket.sendRequest(req);

        if (res != null && res.isSuccess()) {
            showInfo("Thành công", "Mật khẩu đã được cập nhật!");
            // Quay về màn hình Login hoặc đóng form ở đây
            txtOtp.getScene().getWindow().hide();
        } else {
            showError("Lỗi", res != null ? res.getMessage() : "Không nhận được phản hồi từ server!");
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
    // Phương thức báo lỗi
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
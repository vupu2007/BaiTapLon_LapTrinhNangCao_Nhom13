package com.auction.client.controller;

import com.auction.client.ClientConnection;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import com.auction.shared.network.Request;
import com.auction.shared.network.MessageType;
import com.auction.client.MainApp; // Class chứa biến kết nối 'out'

public class ForgotPasswordController {
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;

    @FXML
    public void handleSendOTP() {
        System.out.println("DEBUG: Hàm handleSendOTP đã được gọi!"); // 🚩 Kiểm tra xem nút bấm có ăn không

        String username = txtUsername.getText();
        String email = txtEmail.getText();

        if (username.isEmpty() || email.isEmpty()) {
            System.out.println("DEBUG: Username hoặc Email bị trống!");
            return;
        }

        Request req = new Request(MessageType.FORGOT_PASSWORD, new String[]{username, email});
        ClientConnection.send(req);
        System.out.println("DEBUG: Đã gửi request FORGOT_PASSWORD đi!");
    }
}

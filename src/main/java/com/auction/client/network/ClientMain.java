package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;

public class ClientMain {

    public static void main(String[] args) {
        try {
            // 1. Lấy ra instance duy nhất của ClientSocket và kết nối
            ClientSocket client = ClientSocket.getInstance();
            client.connect();

            // 2. Tạo dữ liệu giả lập để chạy thử tính năng REGISTER lên Server
            String[] testData = {"testuser", "password123", "test@gmail.com"};
            Request msg = new Request(MessageType.REGISTER, testData);

            System.out.println("Client đang gửi yêu cầu Đăng ký thử nghiệm...");
            client.send(msg);

            // 3. Nhận phản hồi dạng Response từ Server gửi về
            Response response = client.receive();

            System.out.println("Server phản hồi thành công? -> " + response.isSuccess());
            System.out.println("Thông báo từ Server: " + response.getMessage());

        } catch(Exception e) {
            System.err.println("Lỗi khi chạy ClientMain: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
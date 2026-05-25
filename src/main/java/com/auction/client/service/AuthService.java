package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import java.util.function.Consumer;

public class AuthService {

    /**
     * 🚀 ĐĂNG KÝ BẤT ĐỒNG BỘ: Chạy ngầm gửi dữ liệu lên Server, bảo vệ UI chống đơ lag
     */
    public void registerAsync(String username, String password, String email, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            Response response = null;
            try {
                // Đóng gói mảng dữ liệu nghiêm túc
                String[] registerData = {username, password, email};
                Request request = new Request(MessageType.REGISTER, registerData);

                // Thực hiện gửi Socket đồng bộ qua cổng mạng ngầm
                response = ClientSocket.getInstance().sendRequest(request);
            } catch (Exception e) {
                System.err.println("❌ [AuthService] Lỗi gửi gói tin đăng ký: " + e.getMessage());
            }

            // Đồng bộ kết quả Response sạch quay ngược lại luồng hiển thị giao diện UI
            final Response finalResponse = response;
            Platform.runLater(() -> callback.accept(finalResponse));
        }, "AuthRegisterWorker");

        worker.setDaemon(true);
        worker.start();
    }
}
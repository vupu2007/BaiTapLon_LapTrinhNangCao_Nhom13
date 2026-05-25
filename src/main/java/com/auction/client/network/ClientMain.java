package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    /**
     * 🚀 VÒNG ĐỜI JAVAFX: Nơi khởi tạo kết nối mạng trước khi vẽ giao diện
     */
    @Override
    public void init() throws Exception {
        System.out.println("🔄 [ClientMain] Đang thiết lập kết nối Socket chạy ngầm...");
        // Khởi động kết nối sớm ở phase init (chạy ngầm, không làm đơ UI Thread)
        ClientSocket.getInstance().connect();
    }

    /**
     * 🎨 VÒNG ĐỜI JAVAFX: Nơi nạp file FXML và hiển thị cửa sổ chính
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Giả lập gửi tin nhắn test nhanh ngay khi vừa mở App (Giữ lại logic test của bạn)
            runSmokeTestAsync();

            // Nạp màn hình đầu tiên của ứng dụng (Ví dụ: Màn hình Đăng nhập Login.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến Real-time");
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ Không thể nạp giao diện chính: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🔌 VÒNG ĐỜI JAVAFX: Tự động kích hoạt khi người dùng tắt App (Bấm nút X)
     */
    @Override
    public void stop() {
        System.out.println("🛑 [ClientMain] Ứng dụng đang đóng. Đang giải phóng tài nguyên mạng...");
        ClientSocket.getInstance().disconnect();
    }

    /**
     * Hàm chạy thử nghiệm gửi gói tin Đăng ký của bạn (Đã đưa vào luồng ngầm để bảo vệ UI)
     */
    private void runSmokeTestAsync() {
        Thread testThread = new Thread(() -> {
            try {
                String[] testData = {"testuser", "password123", "test@gmail.com"};
                Request msg = new Request(MessageType.REGISTER, testData);

                System.out.println("📡 [Test] Client đang gửi yêu cầu Đăng ký thử nghiệm...");
                Response response = ClientSocket.getInstance().sendRequest(msg);

                System.out.println("📩 [Test] Server phản hồi thành công? -> " + response.isSuccess());
                System.out.println("📩 [Test] Thông báo từ Server: " + response.getMessage());
            } catch (Exception e) {
                System.err.println("❌ [Test] Lỗi khi chạy thử nghiệm mạng: " + e.getMessage());
            }
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    /**
     * Cổng vào duy nhất của toàn bộ chương trình Client
     */
    public static void main(String[] args) {
        // Thêm Shutdown Hook phòng hờ trường hợp App bị tắt đột ngột bằng lệnh Task Manager / Kill CMD
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ClientSocket.getInstance().disconnect();
        }));

        // Kích hoạt toàn bộ vòng đời của một ứng dụng đồ họa chuyên nghiệp
        launch(args);
    }
}
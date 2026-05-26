package com.auction.client.network;

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
    public void init() {
        System.out.println("🔄 [ClientMain] Đang thiết lập kết nối Socket chạy ngầm...");
        // Khởi động Engine luồng lắng nghe ngầm của ClientSocket
        ClientSocket.getInstance();
    }

    /**
     * 🎨 VÒNG ĐỜI JAVAFX: Nơi nạp file FXML và hiển thị cửa sổ chính
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Nạp màn hình đầu tiên của ứng dụng (Màn hình Đăng nhập Login.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến Real-time - UET");
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ Không thể nạp giao diện chính: " + e.getMessage());
        }
    }

    /**
     * 🔌 VÒNG ĐỜI JAVAFX: Tự động kích hoạt khi người dùng tắt App (Bấm nút X)
     */
    @Override
    public void stop() {
        System.out.println("🛑 [ClientMain] Ứng dụng đang đóng. Đang giải phóng tài nguyên mạng...");
        ClientSocket.getInstance().closeConnection();
    }

    /**
     * Cổng vào duy nhất của toàn bộ chương trình Client
     */
    public static void main(String[] args) {
        // Thêm Shutdown Hook phòng hờ trường hợp App bị tắt đột ngột bằng lệnh Task Manager
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ClientSocket.getInstance().closeConnection();
        }));

        // Kích hoạt vòng đời ứng dụng đồ họa
        launch(args);
    }
}
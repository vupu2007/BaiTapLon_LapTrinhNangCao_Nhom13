package com.auction.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class SocketServerInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // Khởi tạo một Thread riêng biệt chạy ngầm để mở cổng Socket 12345
        // Việc này giúp cổng Socket hoạt động song song, không làm nghẽn luồng chính của Web Spring Boot (8080)
        Thread socketThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(12345)) {
                System.out.println("====================================================");
                System.out.println("🚀 [Socket Server] Khởi chạy thành công chuẩn kết nối thuần TCP!");
                System.out.println("🔌 Đang lắng nghe yêu cầu từ Client JavaFX tại cổng: 12345");
                System.out.println("====================================================");

                while (true) {
                    // Chờ và chấp nhận kết nối từ phía Client JavaFX thông qua mạng Radmin VPN
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("🔌 [Socket] Có một máy Client JavaFX vừa kết nối tới mạng hệ thống!");

                    // 💡 NƠI GẮN LOGIC XỬ LÝ ĐĂNG NHẬP / ĐẤU GIÁ CỦA SERVER:
                    // Các bạn sẽ xử lý luồng đọc ObjectInputStream và viết ObjectOutputStream ở đây
                    // Hoặc đẩy clientSocket này vào một luồng (Thread/Worker) xử lý riêng biệt.
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi nghiêm trọng khi thiết lập hoặc duy trì Socket Server: " + e.getMessage());
            }
        });

        socketThread.setDaemon(true); // Đảm bảo luồng Socket này sẽ tự tắt khi bạn tắt Server Spring Boot
        socketThread.start();
    }
}
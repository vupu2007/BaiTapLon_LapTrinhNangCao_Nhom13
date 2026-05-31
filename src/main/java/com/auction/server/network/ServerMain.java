package com.auction.server.network;

import com.auction.server.service.AuctionScheduler;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    private static final int PORT = 12345;

    // 🌟 TỐI ƯU 1: Chuyển sang CachedThreadPool để co giãn số luồng linh hoạt theo lượng Client thực tế
    private static final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ClientHandler-Worker");
        t.setDaemon(true); // Đặt làm luồng nền để không chặn tiến trình tắt hệ thống
        return t;
    });

    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = true;
    public static void main(String[] args) {
        // 🌟 TỐI ƯU 2: Đăng ký cơ chế Tắt nguồn mềm (Graceful Shutdown Hook)
        // Hệ thống sẽ tự động bắt được lệnh tắt nguồn (Ctrl+C, tắt Terminal, Kill Process) để dọn dẹp hạ tầng
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 [Server] Đang kích hoạt tiến trình tắt nguồn an toàn (Graceful Shutdown)...");
            isRunning = false;

            // 1. Dừng bộ quét phiên đấu giá ngầm để bảo vệ toàn vẹn dữ liệu Database
            try {
                AuctionScheduler.getInstance().stop();
                System.out.println("✅ [Shutdown] Đã dừng Scheduler an toàn.");
            } catch (Exception e) {
                System.err.println("❌ Lỗi dừng Scheduler: " + e.getMessage());
            }

            // 2. Ép ServerSocket giải phóng luồng accept() đang block
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("✅ [Shutdown] Đã đóng cổng lắng nghe ServerSocket.");
                }
            } catch (Exception ignored) {}

            // 3. Tắt luồng xử lý Thread Pool một cách văn minh
            pool.shutdown(); // Không nhận thêm Client mới, xử lý nốt dữ liệu Client cũ
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Nếu quá 5 giây chưa xong thì ép hủy luồng
                }
                System.out.println("✅ [Shutdown] Đã giải phóng hoàn toàn Thread Pool.");
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }

            System.out.println("=== SERVER ĐÃ ĐÓNG HOÀN TOÀN AN TOÀN ===");
        }, "Server-Shutdown-Hook-Thread"));

        try {
            // Khởi chạy bộ lập lịch đấu giá
            AuctionScheduler.getInstance().start();

            serverSocket = new ServerSocket();

            // 🌟 TỐI ƯU 3: Bật tính năng tái sử dụng cổng mạng ngay lập tức
            // Tránh triệt để lỗi "Address already in use" khi bạn Restart Server liên tục để Test code
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new java.net.InetSocketAddress(PORT));

            System.out.println("=== 🚀 SERVER ĐÃ KHỞI ĐỘNG THÀNH CÔNG TRÊN CỔNG " + PORT + " ===");

            // Sử dụng biến cờ hiệu kết hợp kiểm tra trạng thái Socket để duy trì vòng lặp
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("📡 Client mới kết nối từ: " + socket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(socket);
                    pool.execute(handler);

                } catch (SocketException se) {
                    // Khi serverSocket.close() được gọi ở Shutdown Hook, lệnh accept() sẽ ném ra SocketException.
                    // Đây là hành vi hoàn toàn bình thường khi chủ động tắt Server, ta chỉ cần lờ nó đi.
                    if (!isRunning) {
                        break;
                    }
                    System.err.println("❌ Lỗi kết nối Socket: " + se.getMessage());
                }
            }

        } catch(Exception e) {
            if (isRunning) {
                System.err.println("❌ Lỗi nghiêm trọng tại ServerMain: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
package com.auction.server.network;

import com.auction.server.service.AuctionScheduler;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    // Thread pool giới hạn 10 luồng xử lý client song song
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    private static final int PORT = 12345; // Đồng bộ cứng cổng 12345 với Client

    public static void main(String[] args) {
        try {
            // 🌟 KÍCH HOẠT SCHEDULER: Quét phiên đấu giá chạy ngầm đúng vị trí trên Server
            AuctionScheduler.getInstance().start();

            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("=== SERVER ĐÃ KHỞI ĐỘNG THÀNH CÔNG TRÊN CỔNG " + PORT + " ===");

            // Vòng lặp vô tận lắng nghe Client kết nối
            while(true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client mới kết nối từ: " + socket.getRemoteSocketAddress());

                // Giao Socket cho Handler xử lý riêng biệt
                ClientHandler handler = new ClientHandler(socket);

                // Đẩy vào Thread Pool quản lý tập trung, chống quá tải luồng
                pool.execute(handler);
            }

        } catch(Exception e) {
            System.err.println("Lỗi khởi động ServerMain: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tắt scheduler an toàn khi dừng server
            AuctionScheduler.getInstance().stop();
        }
    }
}
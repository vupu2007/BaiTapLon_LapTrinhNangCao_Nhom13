package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ClientSocket {
    private static ClientSocket instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = true;
    private boolean isConnected = false; // 🎯 Biến cờ kiểm soát trạng thái kết nối vật lý

    // 📦 HỘP THƯ TRUNG CHUYỂN: Nơi các luồng gửi đăng ký đợi phản hồi theo ID
    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    // 🎯 REAL-TIME OBSERVER MAP: Quản lý các Controller chi tiết đang mở theo ID phiên đấu giá
    private final ConcurrentHashMap<Integer, Object> auctionObservers = new ConcurrentHashMap<>();

    private ClientSocket() {
        connectToServer();
    }

    /**
     * 🌐 Thực hiện kết nối vật lý tới hệ thống Server qua mạng
     */
    private synchronized void connectToServer() {
        try {
            String serverHost = "26.59.59.167";
            int serverPort = 12345;

            System.out.println("🔌 [ClientSocket] Đang kết nối tới Server tại " + serverHost + ":" + serverPort + "...");
            this.socket = new Socket(serverHost, serverPort);

            // Khởi tạo luồng ghi dữ liệu trước để tránh deadlock dòng stream
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());

            this.isRunning = true;
            this.isConnected = true;

            // 🚀 KHỞI CHẠY LUỒNG LẮNG NGHE DUY NHẤT
            Thread listenerThread = new Thread(this::listenFromServer);
            listenerThread.setName("SocketListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("✅ [ClientSocket] Kết nối thành công và đã kích hoạt luồng nghe ngầm!");
        } catch (IOException e) {
            this.isConnected = false;
            this.out = null;
            this.in = null;
            System.err.println("❌ [ClientSocket] Kết nối thất bại (Server chưa bật hoặc sai Port): " + e.getMessage());
        }
    }

    /**
     * 🔄 SỬA LỖI ĐÓNG BĂNG SINGLETON:
     * Nếu lần trước kết nối lỗi, lần gọi sau sẽ tự động thử kết nối lại thay vì ôm thực thể lỗi.
     */
    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        } else if (!instance.isConnected) {
            System.out.println("🔄 [ClientSocket] Phát hiện kết nối cũ bị lỗi. Đang thử kết nối lại...");
            instance.connectToServer();
        }
        return instance;
    }

    /**
     * Hàm gửi Request an toàn đa luồng tuyệt đối (Non-blocking Stream)
     */
    public Response sendRequest(Request request) {
        // 🎯 VÁ LỖI DÒNG 115: Thêm null cho đối số thứ 3 (Object data) nhằm chặn đứng crash
        if (!isConnected || out == null) {
            System.err.println("❌ [ClientSocket] Không thể gửi yêu cầu. Đường truyền mạng tới Server đang ngoại tuyến.");
            return new Response(false, "Mất kết nối vật lý tới máy chủ đấu giá! Vui lòng kiểm tra Radmin VPN hoặc Server Backend.", null);
        }

        // 1. Cấp mã định danh độc nhất cho Request
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);

        // 2. Tạo một hộp thư trống chờ sẵn trong Map
        CompletableFuture<Response> futureResponse = new CompletableFuture<>();
        pendingRequests.put(requestId, futureResponse);

        // 3. Đẩy gói tin lên mạng
        try {
            synchronized (out) {
                out.writeObject(request);
                out.flush();
                out.reset(); // Dọn sạch bộ đệm Object để tránh trùng lặp dữ liệu cũ
            }
        } catch (IOException e) {
            pendingRequests.remove(requestId);
            System.err.println("❌ Lỗi gửi dữ liệu dọc đường: " + e.getMessage());
            this.isConnected = false;

            // 🎯 VÁ LỖI DÒNG 85: Thêm null cho đối số thứ 3 (Object data) khi mất kết nối dọc đường
            Response failResponse = new Response(false, "Đường truyền mạng bị đứt đoạn khi đang gửi dữ liệu!", null);
            failResponse.setRequestId(requestId);
            return failResponse;
        }

        // 4. Đứng chờ thư phản hồi bay về đúng hòm thư (Hạn định 10 giây chống treo đứng UI)
        try {
            return futureResponse.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("⚠️ Tác vụ chờ phản hồi quá hạn (Timeout) cho ID: " + requestId);

            // 🎯 VÁ LỖI DÒNG 107: Thêm null cho đối số thứ 3 (Object data) và gán ID để luồng giải phóng
            Response timeoutResponse = new Response(false, "Thời gian phản hồi từ máy chủ quá hạn (Timeout)!", null);
            timeoutResponse.setRequestId(requestId);
            return timeoutResponse;
        } finally {
            pendingRequests.remove(requestId); // Xóa hòm thư sau khi giải quyết xong
        }
    }

    /**
     * 👁️ LUỒNG NGẦM LẮNG NGHE: Đọc data liên tục, phân phối đúng luồng xử lý bằng ID
     */
    private void listenFromServer() {
        while (isRunning) {
            try {
                if (in == null) break;

                Object obj = in.readObject();
                if (obj instanceof Response response) {
                    String requestId = response.getRequestId();

                    // TRƯỜNG HỢP 1: Đây là gói phản hồi trực tiếp cho một Request đã gửi trước đó
                    if (requestId != null && pendingRequests.containsKey(requestId)) {
                        pendingRequests.get(requestId).complete(response);
                    }
                    // TRƯỜNG HỢP 2: Thông báo cập nhật Real-time chủ động từ Server
                    else {
                        handleRealtimeNotification(response);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("❌ Luồng lắng nghe Socket gặp lỗi dứt mạch hoặc Server ngắt kết nối: " + e.getMessage());
                    this.isConnected = false;
                    closeConnection();
                }
                break;
            }
        }
    }

    public void addAuctionObserver(int auctionId, Object observer) {
        if (observer != null) {
            auctionObservers.put(auctionId, observer);
            System.out.println("🎯 [Realtime-Observer] Đã đăng ký lắng nghe biến động cho Auction ID: " + auctionId);
        }
    }

    public void removeAuctionObserver(int auctionId) {
        auctionObservers.remove(auctionId);
        System.out.println("🔌 [Realtime-Observer] Đã hủy lắng nghe Auction ID: " + auctionId);
    }

    public void removeAuctionObserver(int auctionId, Object observer) {
        removeAuctionObserver(auctionId);
    }

    private void handleRealtimeNotification(Response response) {
        if (response != null && "AUCTION_UPDATE".equalsIgnoreCase(response.getType())) {
            System.out.println("📢 [Realtime-Broadcast] Nhận được gói tin cập nhật tự động từ Server!");
        }
    }

    public void closeConnection() {
        try {
            isRunning = false;
            isConnected = false;
            auctionObservers.clear();
            pendingRequests.clear();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🔌 [ClientSocket] Đã đóng toàn bộ kết nối Stream và Socket an toàn.");
        } catch (IOException ignored) {}
        this.out = null;
        this.in = null;
        this.socket = null;
    }
}
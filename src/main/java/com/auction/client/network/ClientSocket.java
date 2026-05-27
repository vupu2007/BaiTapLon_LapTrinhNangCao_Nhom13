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

    // 📦 HỘP THƯ TRUNG CHUYỂN: Nơi các luồng gửi đăng ký đợi phản hồi theo ID
    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    // 🎯 REAL-TIME OBSERVER MAP: Quản lý các Controller chi tiết đang mở theo ID phiên đấu giá
    private final ConcurrentHashMap<Integer, Object> auctionObservers = new ConcurrentHashMap<>();

    private ClientSocket() {
        try {
            // 🌐 MẸO CẤU HÌNH: Thay \"localhost\" bằng IP/Host của Clever Cloud khi Deploy thực tế
            String serverHost = "26.59.59.167";
            int serverPort = 12345;

            System.out.println("🔌 [ClientSocket] Đang kết nối tới Server tại " + serverHost + ":" + serverPort + "...");
            this.socket = new Socket(serverHost, serverPort);

            // Khởi tạo luồng ghi dữ liệu trước để tránh deadlock dòng stream
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());

            // 🚀 KHỞI CHẠY LUỒNG LẮNG NGHE DUY NHẤT: Chuyên trách bóc tách gói tin từ Server
            Thread listenerThread = new Thread(this::listenFromServer);
            listenerThread.setName("SocketListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("✅ [ClientSocket] Kết nối thành công và đã kích hoạt luồng nghe ngầm!");
        } catch (IOException e) {
            System.err.println("❌ [ClientSocket] Không thể kết nối tới Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    /**
     * Hàm gửi Request an toàn đa luồng tuyệt đối (Non-blocking Stream)
     */
    public Response sendRequest(Request request) {
        // 1. Cấp mã định danh độc nhất cho Request
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);

        // 2. Tạo một hộp thư trống chờ sẵn trong Map
        CompletableFuture<Response> futureResponse = new CompletableFuture<>();
        pendingRequests.put(requestId, futureResponse);

        // 3. Đẩy gói tin lên mạng (Chỉ synchronized khối WRITE để tránh đè byte đứt mạch)
        try {
            synchronized (out) {
                out.writeObject(request);
                out.flush();
            }
        } catch (IOException e) {
            pendingRequests.remove(requestId);
            System.err.println("❌ Lỗi gửi dữ liệu: " + e.getMessage());
            return null;
        }

        // 4. Đứng chờ thư phản hồi bay về đúng hòm thư (Hạn định 30 giây chống treo đứng UI)
        try {
            return futureResponse.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("⚠️ Tác vụ chờ phản hồi quá hạn (Timeout) cho ID: " + requestId);
            return null;
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
                // Đọc gói tin thô từ Server (Độc quyền luồng này xử lý đọc)
                Object obj = in.readObject();
                if (obj instanceof Response response) {
                    String requestId = response.getRequestId();

                    // TRƯỜNG HỢP 1: Đây là gói phản hồi trực tiếp cho một Request đã gửi trước đó
                    if (requestId != null && pendingRequests.containsKey(requestId)) {
                        pendingRequests.get(requestId).complete(response);
                    }
                    // TRƯỜNG HỢP 2: Thông báo cập nhật Real-time chủ động từ Server (Không có requestId từ Client)
                    else {
                        handleRealtimeNotification(response);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("❌ Luồng lắng nghe Socket gặp lỗi dứt mạch hoặc Server ngắt kết nối: " + e.getMessage());
                    isRunning = false;
                    closeConnection();
                }
                break;
            }
        }
    }

    /**
     * ⚡ ĐĂNG KÝ OBSERVER: Cho phép AuctionDetailController đăng ký lắng nghe cập nhật theo ID sản phẩm
     */
    public void addAuctionObserver(int auctionId, Object observer) {
        if (observer != null) {
            auctionObservers.put(auctionId, observer);
            System.out.println("🎯 [Realtime-Observer] Đã đăng ký lắng nghe biến động cho Auction ID: " + auctionId);
        }
    }

    /**
     * ⚡ HỦY ĐĂNG KÝ OBSERVER: Giải phóng bộ nhớ khi người dùng tắt trang chi tiết
     */
    public void removeAuctionObserver(int auctionId) {
        auctionObservers.remove(auctionId);
        System.out.println("🔌 [Realtime-Observer] Đã hủy lắng nghe Auction ID: " + auctionId);
    }

    /**
     * 🚀 SỬA LỖI ĐỎ BIÊN DỊCH (Overload): Bản nạp chồng nhận 2 tham số để nuông chiều sự nhác của ông
     */
    public void removeAuctionObserver(int auctionId, Object observer) {
        removeAuctionObserver(auctionId); // Gọi lại hàm xóa theo ID ở trên
    }

    /**
     * Xử lý gói dữ liệu thông báo giá/thông tin mới được Server đẩy chủ động xuống
     */
    private void handleRealtimeNotification(Response response) {
        if (response != null && "AUCTION_UPDATE".equalsIgnoreCase(response.getType())) {
            System.out.println("📢 [Realtime-Broadcast] Nhận được gói tin cập nhật tự động từ Server!");
        }
    }

    /**
     * 🔓 PUBLIC ACCESS: Giúp lớp vòng đời ClientMain có thể dọn dẹp bộ nhớ khi tắt App
     */
    public void closeConnection() {
        try {
            isRunning = false;
            auctionObservers.clear();
            pendingRequests.clear();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("🔌 [ClientSocket] Đã đóng toàn bộ kết nối Stream và Socket an toàn.");
        } catch (IOException ignored) {}
    }
}
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
    private boolean isConnected = false;

    // 📦 HỘP THƯ TRUNG CHUYỂN: Khớp nối Request - Response theo ID
    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Object> auctionObservers = new ConcurrentHashMap<>();

    private ClientSocket() {
        connectToServer();
    }

    /**
     * 🌐 Thực hiện kết nối vật lý tới hệ thống Server
     */
    private synchronized void connectToServer() {
        try {
            String serverHost = "26.59.59.167";
            int serverPort = 12345;

            System.out.println("🔌 [ClientSocket] Đang kết nối tới Server tại " + serverHost + ":" + serverPort + "...");
            this.socket = new Socket(serverHost, serverPort);

            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());

            this.isRunning = true;
            this.isConnected = true;

            Thread listenerThread = new Thread(this::listenFromServer);
            listenerThread.setName("SocketListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("✅ [ClientSocket] Kết nối thành công và đã kích hoạt luồng nghe ngầm!");
        } catch (IOException e) {
            this.isConnected = false;
            this.out = null;
            this.in = null;
            System.err.println("❌ [ClientSocket] Kết nối thất bại: " + e.getMessage());
        }
    }

    /**
     * 🔄 Tự động kết nối lại nếu mạch cũ chết
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
     * Hàm gửi Request an toàn đa luồng bất đồng bộ
     */
    public Response sendRequest(Request request) {
        // 🎯 ĐÃ SỬA: Dùng Constructor 2 tham số mới tối ưu
        if (!isConnected || out == null) {
            System.err.println("❌ [ClientSocket] Không thể gửi yêu cầu. Mạng ngoại tuyến.");
            return new Response(false, "Mất kết nối vật lý tới máy chủ đấu giá! Vui lòng kiểm tra Radmin VPN hoặc Server Backend.");
        }

        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);

        CompletableFuture<Response> futureResponse = new CompletableFuture<>();
        pendingRequests.put(requestId, futureResponse);

        try {
            synchronized (out) {
                out.writeObject(request);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            pendingRequests.remove(requestId);
            System.err.println("❌ Lỗi gửi dữ liệu dọc đường: " + e.getMessage());
            this.isConnected = false;

            // 🎯 ĐÃ SỬA: Bỏ tham số null thừa thãi
            Response failResponse = new Response(false, "Đường truyền mạng bị đứt đoạn khi đang gửi dữ liệu!");
            failResponse.setRequestId(requestId);
            return failResponse;
        }

        try {
            return futureResponse.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("⚠️ Tác vụ chờ phản hồi quá hạn (Timeout) cho ID: " + requestId);

            // 🎯 ĐÃ SỬA: Bỏ tham số null thừa thãi
            Response timeoutResponse = new Response(false, "Thời gian phản hồi từ máy chủ quá hạn (Timeout)!");
            timeoutResponse.setRequestId(requestId);
            return timeoutResponse;
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /**
     * 👁️ LUỒNG NGẦM LẮNG NGHE
     */
    private void listenFromServer() {
        while (isRunning) {
            try {
                if (in == null) break;

                Object obj = in.readObject();
                if (obj instanceof Response response) {
                    String requestId = response.getRequestId();

                    if (requestId != null && pendingRequests.containsKey(requestId)) {
                        pendingRequests.get(requestId).complete(response);
                    } else {
                        handleRealtimeNotification(response);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("❌ Luồng lắng nghe Socket gặp lỗi hoặc Server ngắt kết nối: " + e.getMessage());
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
            System.out.println("🎯 [Realtime-Observer] Đã đăng ký lắng nghe phiên ID: " + auctionId);
        }
    }

    public void removeAuctionObserver(int auctionId) {
        auctionObservers.remove(auctionId);
        System.out.println("🔌 [Realtime-Observer] Đã hủy lắng nghe phiên ID: " + auctionId);
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
package com.auction.client.network;

import com.auction.client.ClientConnection; // Cầu nối đồng bộ ngược sang code cũ
import com.auction.client.controller.AuctionDetailController;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
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

    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Object> auctionObservers = new ConcurrentHashMap<>();

    public interface BidUpdateListener {
        void onBidUpdate(int auctionId, double newPrice, String username);
    }
    private BidUpdateListener bidUpdateListener;
    public void setBidUpdateListener(BidUpdateListener listener) {
        this.bidUpdateListener = listener;
    }

    private Runnable onAuctionUpdate;
    public void setOnAuctionUpdate(Runnable callback) { this.onAuctionUpdate = callback; }

    private ClientSocket() {
        connectToServer();
    }

    private synchronized void connectToServer() {
        try {
            Properties props = new Properties();
            try (InputStream is = getClass().getResourceAsStream("/server.properties")) {
                if (is != null) props.load(is);
            }
            String serverHost = props.getProperty("server.host", "localhost");
            int serverPort = Integer.parseInt(props.getProperty("server.port", "12345"));

            System.out.println("🔌 [ClientSocket] Đang kết nối tới " + serverHost + ":" + serverPort);
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

            System.out.println("✅ [ClientSocket] Kết nối thành công!");
        } catch (IOException e) {
            this.isConnected = false;
            this.out = null;
            this.in = null;
            System.err.println("❌ [ClientSocket] Kết nối thất bại: " + e.getMessage());
        }
    }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        } else if (!instance.isConnected) {
            System.out.println("🔄 [ClientSocket] Phát hiện kết nối cũ bị lỗi. Đang thử kết nối lại...");
            instance.connectToServer();
        }
        return instance;
    }

    public ObjectOutputStream getOutputStream() {
        return this.out;
    }

    public Response sendRequest(Request request) {
        if (!isConnected || out == null) {
            System.err.println("❌ [ClientSocket] Mạng ngoại tuyến.");
            return new Response(false, "Mất kết nối máy chủ!");
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
            return new Response(false, "Lỗi gửi dữ liệu!");
        }

        try {
            // 🎯 Đã loại bỏ khối IF tự ý chuyển giao diện FXML tại đây để chống xung đột UI
            return futureResponse.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            return new Response(false, "Thời gian phản hồi quá hạn!");
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    private void listenFromServer() {
        while (isRunning) {
            try {
                if (in == null) break;

                Object obj = in.readObject();

                // Nếu server trả về Request cũ, bắn sang ClientConnection xử lý tiếp
                if (obj instanceof Request serverReq) {
                    ClientConnection.bridgeServerMessage(serverReq);
                    continue;
                }

                if (obj instanceof Response response) {
                    String requestId = response.getRequestId();

                    // Gửi tín hiệu đồng bộ để kiểm tra thông báo FORGOT_PASSWORD_SUCCESS cũ
                    ClientConnection.bridgeResponseCheck(response);

                    if (requestId != null && pendingRequests.containsKey(requestId)) {
                        pendingRequests.get(requestId).complete(response);
                        handleRealtimeNotification(response);
                    } else {
                        handleRealtimeNotification(response);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("❌ Lỗi luồng lắng nghe: " + e.getMessage());
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
            try {
                Request req = new Request(MessageType.SUBSCRIBE_AUCTION, auctionId);
                req.setRequestId(java.util.UUID.randomUUID().toString());
                synchronized (out) {
                    out.writeObject(req);
                    out.flush();
                    out.reset();
                }
            } catch (Exception e) {
                System.err.println("Lỗi subscribe: " + e.getMessage());
            }
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
        String type = response.getType();
        String msg = response.getMessage();

        // 🎯 Đã loại bỏ khối IF check "OTP_SENT" gây tranh chấp giật màn hình ở đây

        if ("BID_UPDATE".equalsIgnoreCase(type)) {
            Object[] data = (Object[]) response.getData();
            if (data == null) return;
            int auctionId = (int) data[0];
            double newPrice = (double) data[1];
            String username = (String) data[2];

            if (username != null && username.contains("PHIÊN ĐẤU GIÁ BẮT ĐẦU")) {
                if (onAuctionUpdate != null) Platform.runLater(onAuctionUpdate);
                return;
            }

            Object observer = auctionObservers.get(auctionId);
            if (observer instanceof AuctionDetailController) {
                ((AuctionDetailController) observer).update(newPrice, username);
            }
        }
        if ("AUCTION_UPDATE".equalsIgnoreCase(type) || "AUCTION_UPDATE".equalsIgnoreCase(msg)) {
            if (onAuctionUpdate != null) Platform.runLater(onAuctionUpdate);
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
package com.auction.client.network;

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

    // 📦 HỘP THƯ TRUNG CHUYỂN: Khớp nối Request - Response theo ID
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

    /**
     * 🌐 Thực hiện kết nối vật lý tới hệ thống Server
     */
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
            Response response = futureResponse.get(20, TimeUnit.SECONDS);

            // 🎯 TỰ ĐỘNG CHUYỂN MÀN HÌNH NẾU SERVER BÁO OTP_SENT
            if (response != null && "OTP_SENT".equals(response.getMessage())) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/ResetPassword.fxml"));
                        javafx.scene.Parent root = loader.load();

                        // Truyền username qua controller mới (nếu bạn có lưu biến tempUsername)
                        // com.auction.client.controller.ResetPasswordController ctrl = loader.getController();
                        // ctrl.setUsername(this.tempUsername);

                        javafx.stage.Stage stage = (javafx.stage.Stage) javafx.stage.Stage.getWindows()
                                .stream().filter(javafx.stage.Window::isShowing).findFirst().orElse(null);
                        if (stage != null) {
                            stage.setScene(new javafx.scene.Scene(root));
                            stage.show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            return response;

        } catch (Exception e) {
            return new Response(false, "Thời gian phản hồi quá hạn!");
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

                    // 1. Vẫn báo hiệu cho hàm sendRequest biết là đã xong
                    if (requestId != null && pendingRequests.containsKey(requestId)) {
                        pendingRequests.get(requestId).complete(response);

                        // ⚡ CỨU CÁNH: Gọi luôn hàm xử lý notification ở đây
                        // để nó kiểm tra xem có phải là "OTP_SENT" không
                        handleRealtimeNotification(response);
                    }
                    // 2. Nếu là tin nhắn chủ động từ Server (không có requestId)
                    else {
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
        String msg = response.getMessage();
        String type = response.getType();

        // 🚀 XỬ LÝ CHUYỂN MÀN HÌNH QUÊN MẬT KHẨU
        if ("OTP_SENT".equals(msg)) {
            javafx.application.Platform.runLater(() -> {
                try {
                    // Dòng này load file FXML của bạn
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/ResetPassword.fxml"));
                    javafx.scene.Parent root = loader.load();

                    // Lấy Stage hiện tại để thay đổi Scene
                    javafx.stage.Stage stage = (javafx.stage.Stage) javafx.stage.Stage.getWindows().stream().filter(javafx.stage.Window::isShowing).findFirst().orElse(null);
                    if (stage != null) {
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.show();
                    }
                } catch (Exception e) {
                    System.err.println("❌ Lỗi chuyển màn hình: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return;
        }

        // 🚀 XỬ LÝ BID UPDATE CŨ
        if ("BID_UPDATE".equalsIgnoreCase(type)) {
            Object[] data = (Object[]) response.getData();
            if (data == null) return;
            int auctionId = (int) data[0];
            double newPrice = (double) data[1];
            String username = (String) data[2];

            // Nếu là thông báo phiên bắt đầu → refresh dashboard
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
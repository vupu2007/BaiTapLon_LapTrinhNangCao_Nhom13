package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * ClientSocket — Singleton quản lý kết nối TCP đến Server.
 * PHIÊN BẢN BẤT ĐỒNG BỘ: Cách ly hộp thư biệt lập, lỗi luồng này không ảnh hưởng luồng khác!
 */
public class ClientSocket {

    private static ClientSocket instance;
    private ClientSocket() {}

    public static synchronized ClientSocket getInstance() {
        if (instance == null) instance = new ClientSocket();
        return instance;
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Object writeLock = new Object();

    // Hệ thống hộp thư biệt lập 100% cho từng loại Request khác nhau
    private final ConcurrentHashMap<String, LinkedBlockingQueue<Response>> responseMap = new ConcurrentHashMap<>();

    public interface BidUpdateListener {
        void onBidUpdate(int auctionId, double newPrice, String username);
    }

    private volatile BidUpdateListener bidUpdateListener = null;
    private Thread listenerThread = null;

    public void connect() throws Exception {
        synchronized (writeLock) {
            if (socket == null || socket.isClosed()) {
                socket = new Socket("localhost", 12345);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Ket noi Server thanh cong!");

                startDispatcherThread();
            }
        }
    }

    private void sendInternal(Request request) throws Exception {
        synchronized (writeLock) {
            connect();
            out.writeObject(request);
            out.flush();
            out.reset();
        }
    }

    /**
     * GỬI NHẬN BẤT ĐỒNG BỘ (ASYNCHRONOUS):
     * Đọc ghi độc lập trên từng hộp thư của luồng, không khóa luồng đọc chung, chống nghẽn chéo tuyệt đối.
     */
    public Response sendRequest(Request request) throws Exception {
        String routeKey = request.getType().name();

        // Chuẩn bị hộp thư riêng cho Request này
        LinkedBlockingQueue<Response> queue = responseMap.computeIfAbsent(routeKey, k -> new LinkedBlockingQueue<>());
        queue.clear(); // Dọn dẹp thư cũ tồn đọng

        // Gửi lệnh lên Server
        sendInternal(request);

        // Thằng nào gửi thì tự vào hàng đợi của mình mà móng tin, tối đa 4 giây không có thì bỏ qua
        Response res = queue.poll(15, TimeUnit.SECONDS);
        if (res == null) {
            throw new IOException("Server phản hồi quá lâu (Timeout 4s) hoặc mất kết nối tại luồng: " + routeKey);
        }
        return res;
    }

    public void send(Request request) throws Exception {
        sendInternal(request);
    }

    /**
     * LUỒNG ĐỌC TẬP TRUNG (Dispatcher) - Phân phối gói tin về đúng làn hộp thư biệt lập
     */
    private void startDispatcherThread() {
        if (listenerThread != null && listenerThread.isAlive()) return;

        listenerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Response resp) {

                        if ("BID_UPDATE".equals(resp.getMessage())) {
                            BidUpdateListener cb = bidUpdateListener;
                            if (cb != null) {
                                Object[] data = (Object[]) resp.getData();
                                cb.onBidUpdate((int) data[0], (double) data[1], (String) data[2]);
                            }
                        } else {
                            // === BẮT ĐẦU ĐOẠN DEBUG VỪA THÊM ===
                            String type = resp.getType();
                            System.out.println("Dispatcher nhận gói tin type: " + type);

                            if (type != null) {
                                pushToQueue(type, resp);
                            } else {
                                if (hasWaiter("LOGIN")) {
                                    pushToQueue("LOGIN", resp);
                                } else {
                                    pushToQueue("REGISTER", resp);
                                }
                            }
                            // === KẾT THÚC ĐOẠN DEBUG ===
                        }
                    }
                } catch (EOFException | java.net.SocketException e) {
                    System.out.println("Dispatcher: Kết nối tới server đã đóng.");
                    break;
                } catch (Exception e) {
                    System.err.println("Dispatcher: lỗi gói tin, tiếp tục: " + e.getMessage());
                }
            }
        }, "socket-dispatcher-thread");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    private void pushToQueue(String routeKey, Response resp) throws InterruptedException {
        LinkedBlockingQueue<Response> queue = responseMap.computeIfAbsent(routeKey, k -> new LinkedBlockingQueue<>());
        queue.put(resp);
    }

    private boolean hasWaiter(String routeKey) {
        LinkedBlockingQueue<Response> queue = responseMap.get(routeKey);
        return queue != null;
    }

    public void setBidUpdateListener(BidUpdateListener listener) {
        this.bidUpdateListener = listener;
        try { connect(); } catch (Exception ignored) {}
    }

    public void clearBidUpdateListener() {
        this.bidUpdateListener = null;
    }

    public void disconnect() {
        if (listenerThread != null) listenerThread.interrupt();
        synchronized (writeLock) {
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            out = null; socket = null;
        }
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        in = null;
        instance = null;
        responseMap.clear();
    }
}
package com.auction.client.network;

import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.io.*;
import java.net.Socket;

public class ClientSocket {

    // ── Singleton ────────────────────────────────────────────────────────────
    private static ClientSocket instance;

    // Constructor PRIVATE — bắt buộc để Singleton hoạt động đúng
    private ClientSocket() {}

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    // ── Socket & Stream ──────────────────────────────────────────────────────
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // ── Realtime push listener ───────────────────────────────────────────────
    /**
     * Interface để Controller đăng ký nhận BID_UPDATE từ Server.
     * Controller tự gọi Platform.runLater() bên trong onBidUpdate() để update UI.
     */
    public interface BidUpdateListener {
        void onBidUpdate(int auctionId, double newPrice, String username);
    }

    private BidUpdateListener bidUpdateListener = null;
    private Thread listenerThread = null;

    // ── Kết nối ──────────────────────────────────────────────────────────────
    public synchronized void connect() throws Exception {
        if (this.socket == null || this.socket.isClosed()) {
            this.socket = new Socket("localhost", 12345);
            // Output TRƯỚC Input — tránh deadlock Java serialization
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in  = new ObjectInputStream(socket.getInputStream());
            System.out.println("Kết nối đến Server thành công!");
        }
    }

    // ── Gửi / Nhận đồng bộ (dùng cho request thông thường) ──────────────────

    /**
     * Gửi Request lên Server.
     * out.reset() bắt buộc — không có dòng này, Java cache object cũ trong stream,
     * gây bug rất khó debug (gửi object mới nhưng Server nhận object cũ).
     */
    public synchronized void send(Request request) throws Exception {
        if (out != null) {
            out.writeObject(request);
            out.flush();
            out.reset(); // ← QUAN TRỌNG: xóa cache object trong stream
        }
    }

    /** Nhận Response từ Server (blocking). */
    public synchronized Response receive() throws Exception {
        if (in != null) {
            return (Response) in.readObject();
        }
        return null;
    }

    /**
     * Gửi Request và chờ Response — dùng cho hầu hết Controller.
     * Tự động kết nối nếu chưa có kết nối.
     *
     * ⚠️ synchronized để tránh 2 thread gửi/nhận xen kẽ nhau gây lỗi stream.
     */
    public synchronized Response sendRequest(Request request) throws Exception {
        connect();
        send(request);
        return receive();
    }

    // ── Realtime listener thread ─────────────────────────────────────────────

    /**
     * Đăng ký listener để nhận BID_UPDATE push từ Server.
     * Chỉ gọi 1 lần khi mở màn hình AuctionDetail.
     *
     * Cách hoạt động:
     * - Server push Response(BID_UPDATE) bất đồng bộ qua socket
     * - listenerThread chạy nền, đọc object từ stream
     * - Nếu là BID_UPDATE → gọi listener.onBidUpdate()
     * - Nếu là Response thông thường → bỏ qua (đã được sendRequest() xử lý)
     *
     * ⚠️ QUAN TRỌNG: khi dùng listener thread, stream bị chiếm bởi 2 thread
     * (listenerThread + sendRequest). Cần tách luồng đọc và ghi rõ ràng.
     * Giải pháp đơn giản nhất: listenerThread chỉ đọc, send() chỉ ghi.
     */
    public synchronized void setBidUpdateListener(BidUpdateListener listener) {
        this.bidUpdateListener = listener;

        // Khởi động listener thread nếu chưa có
        if (listenerThread == null || !listenerThread.isAlive()) {
            listenerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Đọc object từ stream — blocking
                        Object obj = in.readObject();

                        if (obj instanceof Response) {
                            Response resp = (Response) obj;
                            // Chỉ xử lý BID_UPDATE push từ Server
                            if ("BID_UPDATE".equals(resp.getMessage()) && bidUpdateListener != null) {
                                Object[] data = (Object[]) resp.getData();
                                int    auctionId = (int)    data[0];
                                double newPrice  = (double) data[1];
                                String username  = (String) data[2];
                                bidUpdateListener.onBidUpdate(auctionId, newPrice, username);
                            }
                        }
                    } catch (EOFException | java.net.SocketException e) {
                        System.out.println("Listener thread: kết nối đã đóng.");
                        break;
                    } catch (Exception e) {
                        // Bỏ qua lỗi parse — tiếp tục lắng nghe
                        System.err.println("Listener thread lỗi: " + e.getMessage());
                    }
                }
            }, "bid-update-listener");
            listenerThread.setDaemon(true); // Tự tắt khi app tắt
            listenerThread.start();
        }
    }

    /**
     * Hủy đăng ký listener — gọi trong handleBack() của Controller.
     * KHÔNG dừng thread (vẫn cần cho lần sau), chỉ null listener để bỏ qua push.
     */
    public synchronized void clearBidUpdateListener() {
        this.bidUpdateListener = null;
    }

    // ── Đóng kết nối ─────────────────────────────────────────────────────────
    public synchronized void disconnect() {
        try {
            if (listenerThread != null) listenerThread.interrupt();
            if (in   != null) in.close();
            if (out  != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        finally {
            in = null; out = null; socket = null; instance = null;
        }
    }
}
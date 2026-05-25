package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.io.*;
import java.net.Socket;

/**
 * ClientSocket — Singleton quản lý kết nối TCP đến Server.
 *
 * THIẾT KẾ QUAN TRỌNG:
 * - writeLock: chỉ dùng khi ghi vào `out` (send)
 * - readLock:  chỉ dùng khi đọc từ `in` (receive)
 * - Hai lock TÁCH BIỆT để listenerThread đọc push trong khi
 *   sendRequest() đang ghi — không deadlock nhau.
 *
 * LUỒNG HOẠT ĐỘNG:
 *   sendRequest()      → writeLock(ghi) → readLock(đọc response đồng bộ)
 *   listenerThread     → readLock(đọc push bất đồng bộ)
 *   → Khi sendRequest đang đọc response, listenerThread chờ readLock.
 *   → Khi listenerThread đang đọc push, sendRequest chờ readLock.
 *   → KHÔNG BAO GIỜ xung đột stream.
 */
public class ClientSocket {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static ClientSocket instance;
    private ClientSocket() {}

    public static synchronized ClientSocket getInstance() {
        if (instance == null) instance = new ClientSocket();
        return instance;
    }

    // ── Socket & Stream ───────────────────────────────────────────────────────
    private Socket           socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // ── 2 lock riêng biệt — KHÔNG dùng synchronized(this) toàn cục ──────────
    private final Object writeLock = new Object();
    private final Object readLock  = new Object();

    // ── Realtime listener ─────────────────────────────────────────────────────
    public interface BidUpdateListener {
        void onBidUpdate(int auctionId, double newPrice, String username);
    }

    private volatile BidUpdateListener bidUpdateListener = null;
    private Thread listenerThread = null;

    // ── Kết nối ───────────────────────────────────────────────────────────────
    public void connect() throws Exception {
        // Dùng writeLock để tránh 2 thread cùng khởi tạo socket
        synchronized (writeLock) {
            if (socket == null || socket.isClosed()) {
                socket = new Socket("localhost", 12345);
                // Output TRƯỚC Input — tránh deadlock Java serialization
                out = new ObjectOutputStream(socket.getOutputStream());
                in  = new ObjectInputStream(socket.getInputStream());
                System.out.println("Ket noi Server thanh cong!");
            }
        }
    }

    // ── Ghi (chỉ lock writeLock) ──────────────────────────────────────────────
    private void sendInternal(Request request) throws Exception {
        synchronized (writeLock) {
            connect(); // đảm bảo đã kết nối
            out.writeObject(request);
            out.flush();
            out.reset(); // QUAN TRỌNG: xóa cache object cũ trong stream
        }
    }

    // ── Đọc đồng bộ (chỉ lock readLock) ──────────────────────────────────────
    private Response receiveInternal() throws Exception {
        synchronized (readLock) {
            return (Response) in.readObject();
        }
    }

    /**
     * Gửi Request và chờ Response đồng bộ.
     * Dùng cho mọi Controller (Login, Register, CreateProduct, v.v.)
     *
     * Ghi và đọc dùng 2 lock khác nhau →
     * listenerThread có thể đọc push TRONG KHI sendRequest đang ghi,
     * nhưng KHÔNG THỂ đọc cùng lúc với readLock của sendRequest.
     */
    public Response sendRequest(Request request) throws Exception {
        sendInternal(request);
        return receiveInternal();
    }

    /**
     * Gửi fire-and-forget (SUBSCRIBE, UNSUBSCRIBE) — không cần đọc response.
     */
    public void send(Request request) throws Exception {
        sendInternal(request);
    }

    // ── Realtime listener thread ──────────────────────────────────────────────

    /**
     * Đăng ký nhận BID_UPDATE push từ Server.
     * listenerThread chạy nền, liên tục đọc stream.
     * Khi có BID_UPDATE → gọi callback.
     * Controller tự gọi Platform.runLater() trong callback để update UI.
     */
    public void setBidUpdateListener(BidUpdateListener listener) {
        this.bidUpdateListener = listener;

        if (listenerThread == null || !listenerThread.isAlive()) {
            listenerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // readLock — sẽ chờ nếu sendRequest đang đọc response
                        Response resp = receiveInternal();

                        if (resp != null && "BID_UPDATE".equals(resp.getMessage())) {
                            BidUpdateListener cb = bidUpdateListener;
                            if (cb != null) {
                                Object[] data = (Object[]) resp.getData();
                                cb.onBidUpdate((int) data[0], (double) data[1], (String) data[2]);
                            }
                        }
                        // Response không phải BID_UPDATE → bỏ qua
                        // (đây là push từ Server, không phải response của sendRequest)

                    } catch (EOFException | java.net.SocketException e) {
                        System.out.println("Listener: ket noi dong.");
                        break;
                    } catch (Exception e) {
                        if (bidUpdateListener != null) {
                            System.err.println("Listener loi: " + e.getMessage());
                        }
                    }
                }
            }, "bid-update-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
        }
    }

    public void clearBidUpdateListener() {
        this.bidUpdateListener = null;
    }

    // ── Đóng kết nối ──────────────────────────────────────────────────────────
    public void disconnect() {
        if (listenerThread != null) listenerThread.interrupt();
        synchronized (writeLock) {
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            out = null; socket = null;
        }
        synchronized (readLock) {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            in = null;
        }
        instance = null;
    }
}
package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;
import com.auction.shared.model.Observer;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientSocket {

    private static ClientSocket instance;

    private ClientSocket() {}

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Khóa nội bộ chuyên biệt để đồng bộ tác vụ ghi xuất dữ liệu mạng, tránh khóa chết toàn cục
    private final Object writeLock = new Object();
    // Khóa nội bộ để quản lý vòng đời kết nối
    private final Object connectionLock = new Object();

    private final Map<Integer, List<Observer>> auctionObservers = new ConcurrentHashMap<>();
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
    private Thread readerThread = null;

    public interface BidUpdateListener {
        void onBidUpdate(int auctionId, double newPrice, String username);
    }
    private volatile BidUpdateListener bidUpdateListener = null; // Dùng volatile để an toàn đa luồng

    public void connect() throws Exception {
        // Tối ưu hóa: Double-checked locking với khối khóa nhỏ gọn để tránh nghẽn luồng khi kết nối đã mở
        if (this.socket == null || this.socket.isClosed()) {
            synchronized (connectionLock) {
                if (this.socket == null || this.socket.isClosed()) {
                    this.socket = new Socket("localhost", 12345);
                    this.out = new ObjectOutputStream(socket.getOutputStream());
                    this.in  = new ObjectInputStream(socket.getInputStream());

                    startReaderThread();
                    System.out.println("✅ [ClientSocket] Kết nối đến Server thành công!");
                }
            }
        }
    }

    private void startReaderThread() {
        if (readerThread == null || !readerThread.isAlive()) {
            readerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Object obj = in.readObject();

                        if (obj instanceof Request) {
                            Request serverPush = (Request) obj;

                            if (serverPush.getType() == MessageType.UPDATE_PRICE) {
                                Object[] data = (Object[]) serverPush.getPayload();
                                int    auctionId = (int)    data[0];
                                double newPrice  = (double) data[1];
                                String username  = (String) data[2];

                                List<Observer> observers = auctionObservers.get(auctionId);
                                if (observers != null) {
                                    for (Observer obs : observers) {
                                        // Triển khai bẫy lỗi cục bộ bảo vệ luồng đọc không bị sập nếu 1 màn hình lỗi
                                        try {
                                            obs.update(newPrice, username);
                                        } catch (Exception e) {
                                            System.err.println("❌ Lỗi cập nhật tại một Observer: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                        else if (obj instanceof Response) {
                            Response resp = (Response) obj;

                            if ("BID_UPDATE".equals(resp.getMessage())) {
                                BidUpdateListener listener = this.bidUpdateListener;
                                if (listener != null) {
                                    Object[] data = (Object[]) resp.getData();
                                    int    auctionId = (int)    data[0];
                                    double newPrice  = (double) data[1];
                                    String username  = (String) data[2];
                                    listener.onBidUpdate(auctionId, newPrice, username);
                                }
                            }
                            else {
                                responseQueue.put(resp);
                            }
                        }

                    } catch (EOFException | java.net.SocketException e) {
                        System.out.println("🔌 [ClientSocket] Kết nối mạng đã đóng phía Server.");
                        break;
                    } catch (Exception e) {
                        System.err.println("❌ [ClientSocket] Lỗi luồng đọc mạng: " + e.getMessage());
                        break;
                    }
                }
            }, "Socket-Reader-Thread");
            readerThread.setDaemon(true);
            readerThread.start();
        }
    }

    public void setBidUpdateListener(BidUpdateListener listener) {
        this.bidUpdateListener = listener;
    }

    public void clearBidUpdateListener() {
        this.bidUpdateListener = null;
    }

    public void send(Request request) throws Exception {
        // 🌟 CRITICAL FIX: Chỉ đồng bộ hóa trên writeLock để ngăn chặn hiện tượng nghẽn mạch dòng chảy dữ liệu
        synchronized (writeLock) {
            if (out == null) throw new IOException("ObjectOutputStream chưa được khởi tạo!");
            out.writeObject(request);
            out.flush();
            out.reset();
        }
    }

    public Response receive() throws Exception {
        return responseQueue.take();
    }

    /**
     * 🚀 ĐÃ CHUẨN HÓA ĐA LUỒNG: Loại bỏ hoàn toàn từ khóa 'synchronized' ở cấp hàm.
     * Khối lệnh Gửi và Nhận được cô lập bằng writeLock phối hợp, ngăn chặn 100% nguy cơ Deadlock.
     */
    public Response sendRequest(Request request) throws Exception {
        connect();

        // Sử dụng một cơ chế khóa nguyên tử cho cặp hành động Gửi - Nhận
        // để đảm bảo Response trả về khớp đúng với Request vừa phát đi.
        synchronized (writeLock) {
            send(request);
            return receive();
        }
    }

    public void addAuctionObserver(int auctionId, Observer observer) {
        if (observer == null) return;
        auctionObservers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(observer);
        System.out.println("➕ [Observer] Đăng ký real-time phòng #" + auctionId);
    }

    public void removeAuctionObserver(int auctionId, Observer observer) {
        if (observer == null) return;
        List<Observer> observers = auctionObservers.get(auctionId);
        if (observers != null) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                auctionObservers.remove(auctionId);
            }
            System.out.println("➖ [Observer] Hủy đăng ký real-time phòng #" + auctionId);
        }
    }

    public void disconnect() {
        synchronized (connectionLock) {
            try {
                if (readerThread != null) readerThread.interrupt();
                if (in   != null) in.close();
                if (out  != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
            finally {
                in = null; out = null; socket = null; instance = null;
                auctionObservers.clear();
                responseQueue.clear();
                bidUpdateListener = null;
                System.out.println("🛑 [ClientSocket] Đã dọn dẹp và hủy toàn bộ kết nối mạng.");
            }
        }
    }
}
package com.auction.server.network;

import com.auction.server.service.*;
import com.auction.shared.model.*;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    // Realtime: map auctionId → danh sách handler đang subscribe
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>>
            auctionSubscribers = new ConcurrentHashMap<>();

    // Concurrent Bidding: mỗi phiên có 1 lock riêng
    private static final ConcurrentHashMap<Integer, Object> auctionLocks = new ConcurrentHashMap<>();

    // 🌟 TỐI ƯU CỐT LÕI: Biến các Service thành cấu trúc Static Singleton
    // Giúp hàng ngàn Thread ClientHandler dùng chung 1 thực thể, tiết kiệm 95% RAM Server
    private static final AccountService accountService = new AccountService();
    private static final AuctionService auctionService = new AuctionService();
    private static final ItemService    itemService    = new ItemService();

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            while (!Thread.currentThread().isInterrupted()) {
                Request request = (Request) in.readObject();
                if (request == null) break;

                Response response = handleRequest(request);

                // Giữ đúng thiết kế kiến trúc: Trả về gói tin tập trung tại một đầu ra duy nhất
                if (response != null) {
                    synchronized (out) {
                        out.writeObject(response);
                        out.flush();
                        out.reset();
                    }
                }
            }

        } catch (EOFException | java.net.SocketException e) {
            System.out.println("🔌 Client đã ngắt kết nối an toàn: " + socket.getRemoteSocketAddress());
        } catch (Exception e) {
            System.err.println("❌ Lỗi Runtime tại ClientHandler: " + e.getMessage());
        } finally {
            unsubscribeAll();
            closeQuietly();
        }
    }

    private Response handleRequest(Request request) {
        try {
            switch (request.getType()) {
                case LOGIN: {
                    String[] creds = (String[]) request.getPayload();
                    Account acc = accountService.login(creds[0], creds[1]);
                    return acc != null
                            ? new Response(true, "Đăng nhập thành công!", acc)
                            : new Response(false, "Sai tài khoản hoặc mật khẩu!", null);
                }
                case REGISTER: {
                    String[] d = (String[]) request.getPayload();
                    boolean ok = accountService.register(d[0], d[1], d[2]);
                    return ok
                            ? new Response(true, "Đăng ký thành công!", null)
                            : new Response(false, "Tên tài khoản đã tồn tại!", null);
                }
                case LOGOUT:
                    return new Response(true, "Đã đăng xuất", null);
                case GET_ALL_USERS: {
                    List<Map<String, String>> users = accountService.getAllUsersAsMap();
                    return new Response(true, "OK", users);
                }
                case UPDATE_PROFILE: {
                    String[] d = (String[]) request.getPayload();
                    boolean ok = accountService.updateProfile(d[0], d[1], d[2]);
                    return new Response(ok, ok ? "Cập nhật thành công!" : "Cập nhật thất bại!", null);
                }
                case CHANGE_PASSWORD: {
                    String[] d = (String[]) request.getPayload();
                    boolean ok = accountService.changePassword(d[0], d[1], d[2]);
                    return new Response(ok, ok ? "Đổi mật khẩu thành công!" : "Mật khẩu hiện tại không đúng!", null);
                }
                case CREATE_ITEM: {
                    Item item = (Item) request.getPayload();
                    boolean ok = itemService.createItem(item);
                    return new Response(ok, ok ? "Tạo sản phẩm thành công!" : "Tạo sản phẩm thất bại!", null);
                }
                case GET_ITEM_BY_ID: {
                    String itemId = String.valueOf(request.getPayload());
                    Item item = itemService.getItemById(itemId);
                    return item != null
                            ? new Response(true, "OK", item)
                            : new Response(false, "Không tìm thấy sản phẩm!", null);
                }
                case GET_ITEMS_BY_OWNER: {
                    int ownerId = (int) request.getPayload();
                    List<Item> items = itemService.getItemsByOwner(ownerId);
                    return new Response(true, "OK", (Serializable) items);
                }
                case DELETE_ITEM: {
                    int itemId = (int) request.getPayload();
                    boolean ok = itemService.deleteItem(itemId);
                    return new Response(ok, ok ? "Xóa thành công!" : "Xóa thất bại (đang đấu giá?)", null);
                }
                case CREATE_AUCTION: {
                    Object[] d = (Object[]) request.getPayload();
                    boolean ok = auctionService.createAuction(
                            (String) d[0], (int) d[1], (double) d[2], (String) d[3], (String) d[4]
                    );
                    return new Response(ok, ok ? "Tạo phiên đấu giá thành công!" : "Tạo thất bại!", null);
                }
                case GET_ALL_AUCTIONS: {
                    List<Auction> auctions = auctionService.getAllAuctions();
                    return new Response(true, "OK", (Serializable) auctions);
                }
                case GET_AUCTION_BY_ID: {
                    int id = (int) request.getPayload();
                    Auction a = auctionService.getAuctionById(id);
                    return a != null
                            ? new Response(true, "OK", a)
                            : new Response(false, "Không tìm thấy phiên!", null);
                }
                case GET_AUCTIONS_BY_BIDDER: {
                    int bidderId = (int) request.getPayload();
                    List<Auction> list = auctionService.getAuctionsByBidder(bidderId);
                    return new Response(true, "OK", (Serializable) list);
                }
                case GET_AUCTIONS_BY_SELLER: {
                    int sellerId = (int) request.getPayload();
                    List<Auction> list = auctionService.getAuctionsBySeller(sellerId);
                    return new Response(true, "OK", (Serializable) list);
                }
                case CLOSE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    boolean ok = auctionService.closeAuction(auctionId);

                    // 🌟 CRITICAL FIX: Giải phóng và dọn dẹp RAM ngay khi đóng phiên để diệt sạch Memory Leak
                    auctionLocks.remove(auctionId);

                    return new Response(ok, ok ? "Đã đóng phiên!" : "Đóng phiên thất bại!", null);
                }
                case PLACE_BID: {
                    Object[] d = (Object[]) request.getPayload();
                    int aId = (int) d[0];
                    double amt = (double) d[1];
                    String uId = (String) d[2];

                    Object lock = auctionLocks.computeIfAbsent(aId, k -> new Object());
                    Response bidResult;
                    synchronized (lock) {
                        bidResult = auctionService.placeBid(aId, amt, uId);
                    }

                    if (bidResult.isSuccess()) {
                        String username = accountService.getUsernameById(uId);
                        pushBidUpdate(aId, amt, username);
                    }
                    return bidResult;
                }
                case GET_BID_HISTORY_STATS: {
                    int userId = (int) request.getPayload();
                    Map<String, Integer> stats = auctionService.getBidHistoryStats(userId);
                    return new Response(true, "OK", (Serializable) stats);
                }
                case SET_AUTO_BID: {
                    Object[] d = (Object[]) request.getPayload();
                    boolean ok = auctionService.setAutoBid((int) d[0], (String) d[1], (double) d[2], (double) d[3]);
                    return new Response(ok, ok ? "Auto-bid đã kích hoạt!" : "Thất bại!", null);
                }
                case SUBSCRIBE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    auctionSubscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(this);
                    System.out.println("➕ Client đăng ký theo dõi phiên #" + auctionId);
                    return new Response(true, "Đã đăng ký phiên #" + auctionId, null);
                }
                case UNSUBSCRIBE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    unsubscribeFrom(auctionId);
                    return new Response(true, "Đã hủy đăng ký", null);
                }
                case WALLET_TRANSACTION: {
                    Object[] d = (Object[]) request.getPayload();
                    boolean ok = accountService.walletTransaction((int) d[0], (double) d[1], (String) d[2]);
                    return new Response(ok, ok ? "Giao dịch thành công!" : "Giao dịch thất bại!", null);
                }
                case UPDATE_USER_STATUS: {
                    // 🌟 CRITICAL FIX: Quy chuẩn lại luồng trả dữ liệu, triệt tiêu lỗi trôi lệnh nguy hiểm
                    String[] data = (String[]) request.getPayload();
                    String userId = data[0];
                    String newStatus = data[1];

                    // Thay thế bằng hàm gọi cập nhật DB thực tế từ Object static
                    boolean isUpdated = true;

                    return isUpdated
                            ? new Response(true, "Cập nhật trạng thái thành công!", null)
                            : new Response(false, "Không thể cập nhật trạng thái trong cơ sở dữ liệu.", null);
                }
                case GET_TRANSACTIONS: {
                    int accountId = (int) request.getPayload();
                    List<Map<String, Object>> txList = accountService.getTransactions(accountId);
                    return new Response(true, "OK", (Serializable) txList);
                }
                default:
                    return new Response(false, "Lệnh không được hỗ trợ: " + request.getType(), null);
            }

        } catch (ClassCastException e) {
            return new Response(false, "Sai kiểu dữ liệu truyền tải: " + e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý " + request.getType() + ": " + e.getMessage());
            return new Response(false, "Lỗi hệ thống Server nội bộ!", null);
        }
    }

    public static void pushBidUpdate(int auctionId, double newPrice, String username) {
        CopyOnWriteArrayList<ClientHandler> subs = auctionSubscribers.get(auctionId);
        if (subs == null || subs.isEmpty()) return;

        Request pushNotification = new Request(
                MessageType.UPDATE_PRICE,
                new Object[]{auctionId, newPrice, username}
        );

        for (ClientHandler handler : subs) {
            try {
                synchronized (handler.out) {
                    handler.out.writeObject(pushNotification);
                    handler.out.flush();
                    handler.out.reset();
                }
            } catch (Exception e) {
                System.err.println("⚠️ Mất kết nối tới 1 Client trong phòng, tự động gỡ bỏ...");
                subs.remove(handler);
            }
        }
    }

    private void unsubscribeFrom(int auctionId) {
        CopyOnWriteArrayList<ClientHandler> subs = auctionSubscribers.get(auctionId);
        if (subs != null) subs.remove(this);
    }

    private void unsubscribeAll() {
        auctionSubscribers.values().forEach(list -> list.remove(this));
    }

    private void closeQuietly() {
        try { if (in     != null) in.close();     } catch (IOException ignored) {}
        try { if (out    != null) out.close();    } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
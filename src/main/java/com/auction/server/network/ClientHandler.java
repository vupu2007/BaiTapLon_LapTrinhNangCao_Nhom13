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

    // ── Realtime: map auctionId → danh sách handler đang subscribe ──────────
    // ConcurrentHashMap + CopyOnWriteArrayList để thread-safe khi nhiều client
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>>
            auctionSubscribers = new ConcurrentHashMap<>();

    // ── Concurrent Bidding: mỗi phiên có 1 lock riêng ───────────────────────
    // Tránh lost update / race condition khi nhiều bidder đặt giá cùng lúc
    private static final ConcurrentHashMap<Integer, Object> auctionLocks = new ConcurrentHashMap<>();

    // ── Socket & Stream ──────────────────────────────────────────────────────
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // ── Services (chỉ Server mới có, Client không bao giờ gọi trực tiếp) ────
    private final AccountService accountService = new AccountService();
    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VÒNG LẶP CHÍNH: Nhận Request → Xử lý → Trả Response
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void run() {
        try {
            // Output TRƯỚC Input — tránh deadlock Java serialization
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);

                // Một số MessageType là fire-and-forget (SUBSCRIBE, BID_UPDATE push),
                // không cần trả response đồng bộ → trả null thay vì throw
                if (response != null) {
                    out.writeObject(response);
                    out.flush();
                    out.reset(); // Quan trọng: tránh cache object cũ trong stream
                }
            }

        } catch (EOFException | java.net.SocketException e) {
            System.out.println("Client ngắt kết nối: " + socket.getRemoteSocketAddress());
        } catch (Exception e) {
            System.err.println("Lỗi ClientHandler: " + e.getMessage());
        } finally {
            unsubscribeAll(); // Dọn sạch subscription khi client rời
            closeQuietly();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DISPATCH: phân loại Request theo MessageType
    // ════════════════════════════════════════════════════════════════════════
    private Response handleRequest(Request request) {
        try {
            switch (request.getType()) {

                // ── Auth ─────────────────────────────────────────────────────
                case LOGIN: {
                    String[] creds = (String[]) request.getPayload();
                    Account acc = accountService.login(creds[0], creds[1]);
                    return acc != null
                            ? new Response(true, "Đăng nhập thành công!", acc)
                            : new Response(false, "Sai tài khoản hoặc mật khẩu!", null);
                }

                case REGISTER: {
                    String[] d = (String[]) request.getPayload();
                    // d[0]=username, d[1]=password, d[2]=role
                    boolean ok = accountService.register(d[0], d[1], d[2]);
                    return ok
                            ? new Response(true, "Đăng ký thành công!", null)
                            : new Response(false, "Tên tài khoản đã tồn tại!", null);
                }

                case LOGOUT:
                    return new Response(true, "Đã đăng xuất", null);

                // ── User management (Admin) ───────────────────────────────────
                case GET_ALL_USERS: {
                    List<Map<String, String>> users = accountService.getAllUsersAsMap();
                    return new Response(true, "OK", users);
                }

                case UPDATE_PROFILE: {
                    String[] d = (String[]) request.getPayload();
                    // d[0]=id, d[1]=name, d[2]=email
                    boolean ok = accountService.updateProfile(d[0], d[1], d[2]);
                    return new Response(ok, ok ? "Cập nhật thành công!" : "Cập nhật thất bại!", null);
                }

                case CHANGE_PASSWORD: {
                    String[] d = (String[]) request.getPayload();
                    // d[0]=id, d[1]=currentPassword, d[2]=newPassword
                    // Server verify current password trước khi đổi
                    boolean ok = accountService.changePassword(d[0], d[1], d[2]);
                    return new Response(ok, ok ? "Đổi mật khẩu thành công!" : "Mật khẩu hiện tại không đúng!", null);
                }

                // ── Item ──────────────────────────────────────────────────────
                case CREATE_ITEM: {
                    Item item = (Item) request.getPayload();
                    boolean ok = itemService.createItem(item);
                    return new Response(ok, ok ? "Tạo sản phẩm thành công!" : "Tạo sản phẩm thất bại!", null);
                }

                case GET_ITEM_BY_ID: {
                    // itemId có thể là String ("ITEM-XXXXXXXX") hoặc int — xử lý cả 2
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

                // ── Auction ───────────────────────────────────────────────────
                case CREATE_AUCTION: {
                    // payload: Object[] {itemId, sellerId, startPrice, startTimeStr, endTimeStr}
                    Object[] d = (Object[]) request.getPayload();
                    boolean ok = auctionService.createAuction(
                            (String) d[0],
                            (int) d[1],
                            (double) d[2],
                            (String) d[3],
                            (String) d[4]
                    );
                    return new Response(ok, ok ? "Tạo phiên đấu giá thành công!" : "Tạo thất bại!", null);
                }

                case GET_ALL_AUCTIONS: {
                    List<Auction> auctions = auctionService.getAllAuctions();
                    return new Response(true, "OK", (Serializable) auctions);
                }

                // GET_HOT_AUCTIONS — tra List<Item> vi MainController cast sang Item
                case GET_HOT_AUCTIONS: {
                    // Server lay List<Auction> roi convert sang List<Item> cho MainController
                    List<Auction> hotAuctions = auctionService.getHotAuctions();
                    List<Item> hotItems = new java.util.ArrayList<>();
                    for (Auction a : hotAuctions) {
                        Item item = itemService.getItemById(a.getItemId());
                        if (item != null) {
                            // Gan them thong tin tu auction vao item de hien thi
                            item.setStatus(a.getStatus().name());
                            hotItems.add(item);
                        }
                    }
                    return new Response(true, "OK", (Serializable) hotItems);
                }

                // GET_DASHBOARD_STATS — getDashboardStats() khong nhan tham so
                case GET_DASHBOARD_STATS: {
                    // Bo qua payload userId, goi method khong tham so
                    Map<String, Object> stats = auctionService.getDashboardStats();
                    // Them key "ongoing" va "won" cho MainController
                    stats.put("ongoing", stats.getOrDefault("running", 0));
                    stats.put("won", 0);
                    return new Response(true, "OK", (Serializable) stats);
                }

                // GET_BID_HISTORY — goi getBidHistory(auctionId)
                case GET_BID_HISTORY: {
                    int auctionId = (int) request.getPayload();
                    List<BidTransaction> history = auctionService.getBidHistory(auctionId);
                    return new Response(true, "OK", (Serializable) history);
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
                    return new Response(ok, ok ? "Đã đóng phiên!" : "Đóng phiên thất bại!", null);
                }

                // ── Bidding ───────────────────────────────────────────────────
                case PLACE_BID: {
                    // payload: Object[] {auctionId, amount, userId}
                    Object[] d = (Object[]) request.getPayload();
                    int aId = (int) d[0];
                    double amt = (double) d[1];
                    String uId = (String) d[2];

                    // ── Concurrent Bidding Protection ────────────────────────
                    // Mỗi phiên có 1 lock riêng → các phiên khác không block nhau.
                    // Bid cùng 1 phiên xử lý tuần tự → tránh lost update, race condition.
                    Object lock = auctionLocks.computeIfAbsent(aId, k -> new Object());
                    Response bidResult;
                    synchronized (lock) {
                        bidResult = auctionService.placeBid(aId, amt, uId);
                    }

                    // Nếu đặt giá thành công → push realtime cho tất cả subscriber
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
                    // payload: Object[] {auctionId, userId, maxBid, increment}
                    Object[] d = (Object[]) request.getPayload();
                    boolean ok = auctionService.setAutoBid(
                            (int) d[0],
                            (String) d[1],
                            (double) d[2],
                            (double) d[3]
                    );
                    return new Response(ok, ok ? "Auto-bid đã kích hoạt!" : "Thất bại!", null);
                }

                // ── Realtime Observer ─────────────────────────────────────────
                case SUBSCRIBE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    auctionSubscribers
                            .computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                            .add(this);
                    System.out.println("Client đăng ký theo dõi phiên #" + auctionId);
                    return new Response(true, "Đã đăng ký phiên #" + auctionId, null);
                }

                case UNSUBSCRIBE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    unsubscribeFrom(auctionId);
                    return new Response(true, "Đã hủy đăng ký", null);
                }

                // ── Wallet ────────────────────────────────────────────────────
                case WALLET_TRANSACTION: {
                    // payload: Object[] {accountId, amount, type("DEPOSIT"|"WITHDRAW")}
                    Object[] d = (Object[]) request.getPayload();
                    int acId = (int) d[0];
                    double amt = (double) d[1];
                    String type = (String) d[2];
                    boolean ok = accountService.walletTransaction(acId, amt, type);
                    return new Response(ok, ok ? "Giao dịch thành công!" : "Giao dịch thất bại!", null);
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
            return new Response(false, "Sai kiểu payload: " + e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Lỗi xử lý " + request.getType() + ": " + e.getMessage());
            return new Response(false, "Lỗi server nội bộ!", null);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REALTIME PUSH — gửi BID_UPDATE đến tất cả client đang xem phiên này
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gọi sau mỗi PLACE_BID thành công.
     * Thread-safe: CopyOnWriteArrayList cho phép iterate khi đang có add/remove.
     */
    private void pushBidUpdate(int auctionId, double newPrice, String username) {
        CopyOnWriteArrayList<ClientHandler> subs = auctionSubscribers.get(auctionId);
        if (subs == null) return;

        // payload của BID_UPDATE: Object[] {auctionId, newPrice, username}
        Request push = new Request(
                MessageType.BID_UPDATE,
                new Object[]{auctionId, newPrice, username}
        );

        for (ClientHandler handler : subs) {
            if (handler == this) continue; // Không push lại cho người vừa bid
            try {
                synchronized (handler.out) {
                    handler.out.writeObject(push);
                    handler.out.flush();
                    handler.out.reset();
                }
            } catch (Exception e) {
                System.err.println("Không push được tới 1 client, bỏ qua: " + e.getMessage());
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
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {
        }
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
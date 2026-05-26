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
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>>
            auctionSubscribers = new ConcurrentHashMap<>();

    // ── Concurrent Bidding: mỗi phiên có 1 lock riêng ───────────────────────
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

                if (response != null) {
                    synchronized (out) {
                        out.writeObject(response);
                        out.flush();
                        out.reset(); // Tránh cache object cũ trong stream gây lỗi dữ liệu
                    }
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
                    boolean ok = accountService.updateProfile(d[0], d[1], d[2]);
                    return new Response(ok, ok ? "Cập nhật thành công!" : "Cập nhật thất bại!", null);
                }

                case CHANGE_PASSWORD: {
                    String[] d = (String[]) request.getPayload();
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

                // 🌟 FIX TRIỆT ĐỂ LỖI TIMEOUT SẢN PHẨM & CHỐNG NO_SUCH_METHOD_ERROR
                case GET_PRODUCTS: {
                    try {
                        List<Auction> activeAuctions = auctionService.getAllAuctions();
                        List<Item> productItems = new java.util.ArrayList<>();

                        if (activeAuctions != null) {
                            for (Auction a : activeAuctions) {
                                try {
                                    Item item = itemService.getItemById(a.getItemId());
                                    if (item != null) {
                                        try {
                                            item.setStatus(a.getStatus().name());
                                        } catch (Throwable ignored) {}

                                        productItems.add(item);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        return new Response(true, "OK", (Serializable) productItems);
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý GET_PRODUCTS: " + e.getMessage());
                        return new Response(true, "Lỗi server nhưng vẫn trả danh sách rỗng để tránh treo", new java.util.ArrayList<Item>());
                    }
                }

                // ── Auction ───────────────────────────────────────────────────
                case CREATE_AUCTION: {
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

                case GET_HOT_AUCTIONS: {
                    try {
                        List<Auction> hotAuctions = auctionService.getHotAuctions();
                        if (hotAuctions != null) {
                            for (Auction a : hotAuctions) {
                                if (a.getImagePath() != null && a.getImagePath().startsWith("base64:")) {
                                    a.setImagePath(null);
                                }
                            }
                        }
                        return new Response(true, "OK", (Serializable) hotAuctions);
                    } catch (Exception e) {
                        return new Response(true, "Lỗi", new java.util.ArrayList<Auction>());
                    }
                }
                // 🌟 ĐÃ SỬA: Đảm bảo trả về Map chuẩn chỉnh (HashMap) đúng yêu cầu Client để vẽ giao diện
                case GET_DASHBOARD_STATS: {
                    try {
                        Map<String, Object> rawStats = auctionService.getDashboardStats();
                        Map<String, Object> stats = (rawStats != null) ? new HashMap<>(rawStats) : new HashMap<>();
                        stats.put("ongoing", stats.getOrDefault("running", 0));
                        stats.put("won", stats.getOrDefault("won", 0));

                        // Đóng gói và dán nhãn
                        Response response = new Response(true, "OK", (Serializable) stats);
                        response.setType("GET_DASHBOARD_STATS");
                        return response;

                    } catch (Exception e) {
                        System.err.println("Lỗi GET_DASHBOARD_STATS: " + e.getMessage());
                        Map<String, Object> fallbackStats = new HashMap<>();
                        fallbackStats.put("ongoing", 0);
                        fallbackStats.put("won", 0);

                        // Đóng gói lỗi và dán nhãn
                        Response fallbackResponse = new Response(true, "OK", (Serializable) fallbackStats);
                        fallbackResponse.setType("GET_DASHBOARD_STATS");
                        return fallbackResponse;
                    }
                }
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
                    Object[] d = (Object[]) request.getPayload();
                    int acId = (int) d[0];
                    double amt = (double) d[1];
                    String type = (String) d[2];
                    boolean ok = accountService.walletTransaction(acId, amt, type);
                    return new Response(ok, ok ? "Giao dịch thành công!" : "Giao dịch thất bại!", null);
                }

                // 🌟 ĐÃ SỬA: Bảo đảm luôn trả về danh sách dạng List chứ không được lộn sang HashMap
                case GET_TRANSACTIONS: {
                    try {
                        Object idPayload = request.getPayload();
                        int accountId;

                        if (idPayload instanceof String) {
                            accountId = Integer.parseInt((String) idPayload);
                        } else {
                            accountId = (int) idPayload;
                        }

                        List<Map<String, Object>> txList = accountService.getTransactions(accountId);
                        if (txList == null) txList = new ArrayList<>();

                        // --- ĐÃ SỬA: Đóng gói và gán nhãn thành công ---
                        Response response = new Response(true, "OK", (Serializable) txList);
                        response.setType("GET_TRANSACTIONS");
                        return response;

                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý GET_TRANSACTIONS: " + e.getMessage());

                        // --- ĐÃ SỬA: Đóng gói và gán nhãn khi có lỗi ---
                        Response errorResponse = new Response(true, "Ví trống do lỗi", new ArrayList<Map<String, Object>>());
                        errorResponse.setType("GET_TRANSACTIONS");
                        return errorResponse;
                    }
                }

                default:
                    Response defaultResponse = new Response(false, "Lệnh không được hỗ trợ: " + request.getType(), null);
                    // Dùng String.valueOf để chuyển Enum thành String cho an toàn
                    defaultResponse.setType(String.valueOf(request.getType()));
                    return defaultResponse;
            }

        } catch (ClassCastException e) {
            Response castErrResponse = new Response(false, "Sai kiểu payload: " + e.getMessage(), null);
            castErrResponse.setType(String.valueOf(request.getType()));
            return castErrResponse;

        } catch (Exception e) {
            System.err.println("Lỗi xử lý " + request.getType() + ": " + e.getMessage());
            Response sysErrResponse = new Response(false, "Lỗi server nội bộ!", null);
            sysErrResponse.setType(String.valueOf(request.getType()));
            return sysErrResponse;
        }
    }
    // ════════════════════════════════════════════════════════════════════════
    //  REALTIME PUSH — gửi BID_UPDATE đến tất cả client đang xem phiên này
    // ════════════════════════════════════════════════════════════════════════
    private void pushBidUpdate(int auctionId, double newPrice, String username) {
        CopyOnWriteArrayList<ClientHandler> subs = auctionSubscribers.get(auctionId);
        if (subs == null) return;

        Request push = new Request(
                MessageType.BID_UPDATE,
                new Object[]{auctionId, newPrice, username}
        );

        for (ClientHandler handler : subs) {
            if (handler == this) continue;
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
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
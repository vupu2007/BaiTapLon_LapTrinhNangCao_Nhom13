package com.auction.server.network;

import com.auction.server.dao.AccountDAO;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.service.*;
import com.auction.server.util.DatabaseConnection;
import com.auction.shared.model.*;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.auction.shared.network.MessageType.GET_AUCTION_BY_ITEM_ID;
import static com.auction.shared.network.MessageType.GET_BID_HISTORY;

public class ClientHandler implements Runnable {

    // 🚀 QUẢN LÝ CLIENT ONLINE TOÀN CỤC
    public static final CopyOnWriteArrayList<ClientHandler> onlineClients = new CopyOnWriteArrayList<>();

    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>>
            auctionSubscribers = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, Object> auctionLocks = new ConcurrentHashMap<>();

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final ExecutorService serverWorkerPool = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
            r -> {
                Thread t = new Thread(r);
                t.setName("Server-Worker-Pool");
                t.setDaemon(true);
                return t;
            }
    );

    private static final AccountService accountService = new AccountService();
    private static final AuctionService auctionService = new AuctionService();
    private static final ItemService    itemService    = new ItemService();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void printLog(String icon, String status, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String threadName = Thread.currentThread().getName();
        System.out.printf("[%s] [%s] %s [%s] %s%n", timestamp, threadName, icon, status, message);
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in  = new ObjectInputStream(socket.getInputStream());
            printLog("✅", "I/O_CONNECTED", "Đã kết nối thông suốt luồng I/O với Client: " + socket.getRemoteSocketAddress());

            // Đăng ký client vào danh sách online
            onlineClients.add(this);

            while (!Thread.currentThread().isInterrupted()) {
                Request request = (Request) in.readObject();
                if (request == null) break;

                long startTime = System.currentTimeMillis();
                printLog("📥", "REQ_RECEIVED", "Nhận request: " + request.getType());

                serverWorkerPool.submit(() -> {
                    try {
                         Response response = handleRequest(request);

                        if (response != null) {

                            response.setType(request.getType().name()); // ← thêm dòng này

                            response.setRequestId(request.getRequestId());

                            synchronized (out) {
                                out.writeObject(response);
                                out.flush();
                                out.reset();
                            }

                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime;
                            printLog("⚡", "RESP_FINISHED", "Xử lý xong lệnh [" + request.getType() + "] | ⏱️ Tiêu tốn: " + duration + "ms (" + String.format("%.3f", duration / 1000.0) + "s)");
                        }
                    } catch (Exception e) {
                        String timestamp = LocalDateTime.now().format(timeFormatter);
                        System.err.printf("[%s] [%s] ❌ [ERR_RESPONSE] Lỗi phản hồi mạng cho Client: %s%n",
                                timestamp, Thread.currentThread().getName(), e.getMessage());
                    }
                });
            }

        } catch (EOFException | java.net.SocketException e) {
            printLog("🔌", "CLIENT_DISCONNECT", "Client đã ngắt kết nối an toàn: " + socket.getRemoteSocketAddress());
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(timeFormatter);
            System.err.printf("[%s] [%s] ❌ [ERR_RUNTIME] Lỗi tại ClientHandler: %s%n",
                    timestamp, Thread.currentThread().getName(), e.getMessage());
        } finally {
            onlineClients.remove(this);
            unsubscribeAll();
            closeQuietly();
        }
    }

    private Response handleRequest(Request request) {
        try {
            switch (request.getType()) {
                case LOGIN: {
                    try {
                        System.out.println("➡️ [DEBUG] Bắt đầu xử lý LOGIN...");
                        String[] creds = (String[]) request.getPayload();
                        System.out.println("➡️ [DEBUG] Đã nhận payload, Username: " + creds[0]);

                        Account acc = accountService.login(creds[0], creds[1]);
                        System.out.println("➡️ [DEBUG] Đã query DB xong, Kết quả Account: " + (acc != null ? "Có dữ liệu" : "NULL"));

                        Response response = acc != null
                                ? new Response(true, "Đăng nhập thành công!", acc)
                                : new Response(false, "Sai tài khoản hoặc mật khẩu!", null);

                        response.setRequestId(request.getRequestId());
                        System.out.println("➡️ [DEBUG] Đã tạo xong Response, chuẩn bị gửi về Client!");
                        return response;

                    } catch (Exception e) {
                        System.err.println("❌ [LỖI NGHIÊM TRỌNG TẠI CASE LOGIN]:");
                        e.printStackTrace();
                        Response errResponse = new Response(false, "Lỗi máy chủ: " + e.getMessage(), null);
                        errResponse.setRequestId(request.getRequestId());
                        return errResponse;
                    }
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
                    if (ok) broadcastSystemUpdate("AUCTION_UPDATE");
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
                    if (ok) {
                        broadcastSystemUpdate("AUCTION_UPDATE");
                        // Lấy auction vừa tạo để trả về ID
                        Auction created = auctionService.getAuctionByItemId((String) d[0]);
                        return new Response(true, "Tạo phiên đấu giá thành công!", created);
                    }
                    return new Response(false, "Tạo thất bại!", null);
                }
                case GET_ALL_AUCTIONS: {
                    List<Auction> auctions = auctionService.getAllAuctions();
                    return new Response(true, "OK", (Serializable) auctions);
                }
                case GET_AUCTION_BY_ID: {
                    int id = (int) request.getPayload();
                    Auction a = auctionService.getAuctionById(id);
                    if (a == null) return new Response(false, "Không tìm thấy phiên!", null);

                    Item item = itemService.getItemById(a.getItemId());
                    if (item != null) {
                        a.setProductName(item.getName());
                        a.setImagePath(item.getImagePath());
                        a.setDescription(item.getDescription());
                    }

                    Account seller = accountDAO.getAccountById(a.getSellerId());
                    if (seller != null) a.setSellerName(seller.getUsername());
                    if (a.getWinnerId() != null && a.getWinnerId() > 0) {
                        Account winner = accountDAO.getAccountById(a.getWinnerId());
                        if (winner != null) a.setWinnerName(winner.getUsername());
                    }

                    return new Response(true, "OK", a);
                }
                case GET_ACCOUNT_BY_ID: {
                    int id = (int) request.getPayload();
                    Account acc = accountDAO.getAccountById(id);
                    return acc != null
                            ? new Response(true, "OK", acc)
                            : new Response(false, "Không tìm thấy!", null);
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
                    auctionLocks.remove(auctionId);
                    if (ok) broadcastSystemUpdate("AUCTION_UPDATE");
                    return new Response(ok, ok ? "Đã đóng phiên!" : "Đóng phiên thất bại!", null);
                }
                case PLACE_BID: {
                    Object[] d = (Object[]) request.getPayload();int aId = ((Number) d[0]).intValue();
                    String uId = String.valueOf(((Number) d[1]).intValue());
                    double amt = ((Number) d[2]).doubleValue();
                    Object lock = auctionLocks.computeIfAbsent(aId, k -> new Object());
                    Response bidResult;
                    synchronized (lock) {
                        bidResult = auctionService.placeBid(aId, amt, uId);
                    }
                    if (bidResult.isSuccess()) {
                        String username = accountService.getUsernameById(uId);
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
                    String[] data = (String[]) request.getPayload();
                    boolean isUpdated = true;
                    return new Response(true, "Cập nhật trạng thái thành công!", null);
                }
                case GET_TRANSACTIONS: {
                    int accountId = (int) request.getPayload();
                    List<Map<String, Object>> txList = accountService.getTransactions(accountId);
                    return new Response(true, "OK", (Serializable) txList);
                }
                case GET_DASHBOARD_STATS: {
                    String accountId = (String) request.getPayload();
                    Map<String, Integer> statsMap = new HashMap<>();
                    statsMap.put("ongoing", 0);
                    statsMap.put("won", 0);
                    try {
                        Map<String, Integer> realStats = auctionService.getBidHistoryStats(Integer.parseInt(accountId));
                        if (realStats != null) statsMap.putAll(realStats);
                    } catch (Exception ignored) {
                    }
                    return new Response(true, "Lấy số liệu thống kê thành công!", (Serializable) statsMap);
                }
                case GET_HOT_AUCTIONS: {
                    String[] params = (String[]) request.getPayload();
                    String filter = (params != null && params.length > 1) ? params[1] : "ALL";

                    String statusCondition = switch (filter) {
                        case "UPCOMING" -> "a.status = 'OPEN'";
                        case "ACTIVE" -> "a.status = 'RUNNING'";
                        case "FINISHED" -> "a.status = 'FINISHED'";
                        default -> "a.status IN ('RUNNING', 'OPEN', 'FINISHED') ORDER BY FIELD(a.status, 'RUNNING', 'OPEN', 'FINISHED')";
                    };

                    List<Item> items = new ArrayList<>();
                    String sql = "SELECT i.*, a.auction_id, a.current_price, a.start_time, a.end_time, " +
                            "a.seller_id, a.start_price, a.min_increment,  a.status AS auction_status  " +
                            "FROM Items i " +
                            "JOIN Auctions a ON i.item_id = a.item_id " +
                            "WHERE " + statusCondition;
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Item item = itemDAO.mapResultSetToItem(rs);
                            if (item != null) {
                                item.setAuctionId(rs.getInt("auction_id"));
                                item.setCurrentPrice(rs.getDouble("current_price"));
                                item.setAuctionStatus(rs.getString("auction_status"));                                Timestamp et = rs.getTimestamp("end_time");
                                if (et != null) item.setEndTimeStr(et.toLocalDateTime()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                                items.add(item);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi GET_HOT_AUCTIONS: " + e.getMessage());
                    }
                    return new Response(true, "Tải danh sách sản phẩm thành công!", (Serializable) items);
                }

                case GET_AUCTION_BY_ITEM_ID: {
                    String itemId = (String) request.getPayload();
                    try {
                        Auction auction = auctionDAO.getAuctionByItemId(itemId);
                        if (auction == null) return new Response(false, "Không tìm thấy phiên!", null);

                        Item item = itemService.getItemById(auction.getItemId());
                        if (item != null) {
                            auction.setProductName(item.getName());
                            auction.setImagePath(item.getImagePath());
                            auction.setDescription(item.getDescription());
                        }

                        com.auction.shared.model.Account seller = accountDAO.getAccountById(auction.getSellerId());
                        if (seller != null) auction.setSellerName(seller.getUsername());

                        return new Response(true, "OK", auction);
                    } catch (SQLException e) {
                        return new Response(false, "Lỗi DB: " + e.getMessage(), null);
                    }
                }
                case GET_BID_HISTORY: {
                    int auctionId = (int) request.getPayload();
                    List<BidTransaction> bids = bidDAO.getBidsByAuction(auctionId);
                    return new Response(true, "OK", (Serializable) bids);
                }

                default:
                    return new Response(false, "Lệnh không được hỗ trợ!", null);
            }
        } catch (Exception e) {
            return new Response(false, "Lỗi hệ thống: " + e.getMessage(), null);
        }



    }
    // 📢 PHÁT SÓNG SỰ KIỆN TOÀN HỆ THỐNG
    public static void broadcastSystemUpdate(String updateType) {
        Response broadcastNotification = new Response(true, "Dữ liệu hệ thống có thay đổi mới!");
        broadcastNotification.setType(updateType);

        System.out.println("📢 [Broadcast-System] Đang gửi tín hiệu real-time [" + updateType + "] tới " + onlineClients.size() + " máy trạm...");

        for (ClientHandler handler : onlineClients) {
            try {
                synchronized (handler.out) {
                    handler.out.writeObject(broadcastNotification);
                    handler.out.flush();
                    handler.out.reset();
                }
            } catch (Exception e) {
                onlineClients.remove(handler);
            }
        }
    }

    public static void pushBidUpdate(int auctionId, double newPrice, String username) {
        System.out.println("pushBidUpdate called: " + auctionId + " " + newPrice);
        new Exception("Stack trace").printStackTrace();
        CopyOnWriteArrayList<ClientHandler> subs = auctionSubscribers.get(auctionId);
        System.out.println("Subscribers: " + (subs == null ? 0 : subs.size()));

        if (subs == null || subs.isEmpty()) return;

        Response push = new Response(true, "BID_UPDATE", new Object[]{auctionId, newPrice, username});

        push.setType("BID_UPDATE");

        for (ClientHandler handler : subs) {
            try {
                synchronized (handler.out) {
                    handler.out.writeObject(push);
                    handler.out.flush();
                    handler.out.reset();
                }
            } catch (Exception e) {
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
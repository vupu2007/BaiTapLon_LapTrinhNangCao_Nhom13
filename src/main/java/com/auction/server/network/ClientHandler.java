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

    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>>
            auctionSubscribers = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, Object> auctionLocks = new ConcurrentHashMap<>();

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private final AccountService accountService = new AccountService();
    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (!Thread.currentThread().isInterrupted()) {
                Request request = (Request) in.readObject();
                if (request == null) continue;

                // Xử lý bất đồng bộ hoàn toàn để chống treo luồng Socket chính
                handleRequestAsync(request);
            }

        } catch (EOFException | java.net.SocketException e) {
            System.out.println("Client ngắt kết nối: " + socket.getRemoteSocketAddress());
        } catch (Exception e) {
            System.err.println("Lỗi ClientHandler: " + e.getMessage());
        } finally {
            unsubscribeAll();
            closeQuietly();
        }
    }

    private void handleRequestAsync(Request request) {
        // Tạo luồng xử lý riêng cho từng gói tin, Database nghẽn luồng này thì luồng khác vẫn chạy
        new Thread(() -> {
            try {
                Response response = handleRequest(request);
                if (response != null) {
                    sendResponse(response);
                }
            } catch (Exception e) {
                System.err.println("Lỗi trong luồng xử lý async: " + e.getMessage());
            }
        }).start();
    }

    private void sendResponse(Response response) {
        synchronized (out) {
            try {
                out.writeObject(response);
                out.flush();
                out.reset();
            } catch (IOException e) {
                System.err.println("Không thể gửi phản hồi về Client: " + e.getMessage());
            }
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
                    return new Response(true, "OK", (Serializable) users);
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
                    if (items == null) items = new ArrayList<>();
                    return new Response(true, "OK", (Serializable) items);
                }

                case DELETE_ITEM: {
                    int itemId = (int) request.getPayload();
                    boolean ok = itemService.deleteItem(itemId);
                    return new Response(ok, ok ? "Xóa thành công!" : "Xóa thất bại (đang đấu giá?)", null);
                }

                case GET_PRODUCTS: {
                    List<Item> productItems = new java.util.ArrayList<>();
                    try {
                        List<Auction> activeAuctions = auctionService.getAllAuctions();
                        if (activeAuctions != null) {
                            for (Auction a : activeAuctions) {
                                Item item = itemService.getItemById(a.getItemId());
                                if (item != null) {
                                    try { item.setStatus(a.getStatus().name()); } catch (Throwable ignored) {}
                                    productItems.add(item);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý GET_PRODUCTS: " + e.getMessage());
                    }
                    return new Response(true, "OK", (Serializable) productItems);
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
                    if (auctions == null) auctions = new ArrayList<>();
                    return new Response(true, "OK", (Serializable) auctions);
                }

                case GET_HOT_AUCTIONS: {
                    List<Item> hotItems = new java.util.ArrayList<>();
                    try {
                        List<Auction> hotAuctions = auctionService.getHotAuctions();
                        if (hotAuctions != null) {
                            for (Auction a : hotAuctions) {
                                Item item = itemService.getItemById(a.getItemId());
                                if (item != null) {
                                    try { item.setStatus(a.getStatus().name()); } catch (Throwable ignored) {}
                                    hotItems.add(item);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý GET_HOT_AUCTIONS: " + e.getMessage());
                    }
                    return new Response(true, "OK", (Serializable) hotItems);
                }

                case GET_DASHBOARD_STATS: {
                    Map<String, Object> stats = new HashMap<>();
                    try {
                        Map<String, Object> rawStats = auctionService.getDashboardStats();
                        if (rawStats != null) stats.putAll(rawStats);
                        stats.put("ongoing", stats.getOrDefault("running", 0));
                        stats.put("won", stats.getOrDefault("won", 0));
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý GET_DASHBOARD_STATS: " + e.getMessage());
                        stats.put("ongoing", 0);
                        stats.put("won", 0);
                    }
                    return new Response(true, "OK", (Serializable) stats);
                }

                case GET_BID_HISTORY: {
                    int auctionId = (int) request.getPayload();
                    List<BidTransaction> history = auctionService.getBidHistory(auctionId);
                    if (history == null) history = new ArrayList<>();
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
                    if (list == null) list = new ArrayList<>();
                    return new Response(true, "OK", (Serializable) list);
                }

                case GET_AUCTIONS_BY_SELLER: {
                    int sellerId = (int) request.getPayload();
                    List<Auction> list = auctionService.getAuctionsBySeller(sellerId);
                    if (list == null) list = new ArrayList<>();
                    return new Response(true, "OK", (Serializable) list);
                }

                case CLOSE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    boolean ok = auctionService.closeAuction(auctionId);
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
                    if (stats == null) stats = new HashMap<>();
                    return new Response(true, "OK", (Serializable) stats);
                }

                case SET_AUTO_BID: {
                    Object[] d = (Object[]) request.getPayload();
                    boolean ok = auctionService.setAutoBid(
                            (int) d[0], (String) d[1], (double) d[2], (double) d[3]
                    );
                    return new Response(ok, ok ? "Auto-bid đã kích hoạt!" : "Thất bại!", null);
                }

                case SUBSCRIBE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    auctionSubscribers
                            .computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                            .add(this);
                    return new Response(true, "Đã đăng ký phiên #" + auctionId, null);
                }

                case UNSUBSCRIBE_AUCTION: {
                    int auctionId = (int) request.getPayload();
                    unsubscribeFrom(auctionId);
                    return new Response(true, "Đã hủy đăng ký", null);
                }

                case WALLET_TRANSACTION: {
                    Object[] d = (Object[]) request.getPayload();
                    int acId = (int) d[0];
                    double amt = (double) d[1];
                    String type = (String) d[2];
                    boolean ok = accountService.walletTransaction(acId, amt, type);
                    return new Response(ok, ok ? "Giao dịch thành công!" : "Giao dịch thất bại!", null);
                }

                case GET_TRANSACTIONS: {
                    List<Map<String, Object>> txList = new ArrayList<>();
                    try {
                        Object idPayload = request.getPayload();
                        int accountId = (idPayload instanceof String)
                                ? Integer.parseInt((String) idPayload)
                                : (int) idPayload;

                        List<Map<String, Object>> list = accountService.getTransactions(accountId);
                        if (list != null) txList.addAll(list);
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý GET_TRANSACTIONS: " + e.getMessage());
                    }
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

    private void pushBidUpdate(int auctionId, double newPrice, String username) {
        CopyOnWriteArrayList<ClientHandler> subs = auctionSubscribers.get(auctionId);
        if (subs == null) return;

        Request push = new Request(MessageType.BID_UPDATE, new Object[]{auctionId, newPrice, username});

        for (ClientHandler handler : subs) {
            if (handler == this) continue;
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
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
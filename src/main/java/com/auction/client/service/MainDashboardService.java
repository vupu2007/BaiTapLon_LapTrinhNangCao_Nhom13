package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.model.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class MainDashboardService {

    // 🚀 TỐI ƯU HẠ TẦNG: Tạo một hồ cấp phát luồng (Thread Pool) ngầm cố định tại Client để bắn request song song
    private static final ExecutorService networkExecutor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r);
        t.setName("DashboardNetworkWorker");
        t.setDaemon(true); // Tự giải phóng luồng khi tắt ứng dụng
        return t;
    });

    /**
     * Tải dữ liệu thống kê và danh sách sản phẩm SONG SONG từ Server (Triệt tiêu thời gian chờ cộng dồn)
     * @param accountId ID của tài khoản đang đăng nhập
     * @param filter Bộ lọc hiển thị ("ALL", "ACTIVE", "UPCOMING")
     * @param callback Hàm callback cập nhật giao diện JavaFX sau khi nhận được phản hồi
     */
    @SuppressWarnings("unchecked")
    public void fetchDashboardDataAsync(String accountId, String filter, BiConsumer<Map<String, Integer>, List<Item>> callback) {

        // -------------------------------------------------------------------------
        // 📊 LUỒNG SONG SONG 1: Gọi lệnh lấy số liệu thống kê (GET_DASHBOARD_STATS)
        // -------------------------------------------------------------------------
        CompletableFuture<Map<String, Integer>> statsTask = CompletableFuture.supplyAsync(() -> {
            try {
                ClientSocket socketHandler = ClientSocket.getInstance();
                Request statsRequest = new Request(MessageType.GET_DASHBOARD_STATS, accountId);

                System.out.println("⚡ [Parallel-Net] Luồng 1 xuất phát bắn request: GET_DASHBOARD_STATS");
                Response statsResponse = socketHandler.sendRequest(statsRequest);

                if (statsResponse != null && statsResponse.isSuccess()) {
                    return (Map<String, Integer>) statsResponse.getData();
                } else {
                    System.err.println("⚠️ [Service Log] Server từ chối trả Stats hoặc phản hồi thất bại!");
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi trong luồng lấy Stats: " + e.getMessage());
            }
            return null;
        }, networkExecutor);

        // -------------------------------------------------------------------------
        // 🔥 LUỒNG SONG SONG 2: Gọi lệnh lấy danh sách sản phẩm (GET_HOT_AUCTIONS)
        // -------------------------------------------------------------------------
        CompletableFuture<List<Item>> itemsTask = CompletableFuture.supplyAsync(() -> {
            List<Item> itemsResult = new ArrayList<>();
            try {
                ClientSocket socketHandler = ClientSocket.getInstance();
                String[] payloadPack = new String[]{ accountId, filter };
                Request itemsRequest = new Request(MessageType.GET_HOT_AUCTIONS, payloadPack);

                System.out.println("⚡ [Parallel-Net] Luồng 2 xuất phát bắn request: GET_HOT_AUCTIONS với Filter: " + filter);
                Response itemsResponse = socketHandler.sendRequest(itemsRequest);

                if (itemsResponse != null && itemsResponse.isSuccess()) {
                    List<Item> dbItems = (List<Item>) itemsResponse.getData();
                    if (dbItems != null) {
                        itemsResult.addAll(dbItems);
                    }
                } else {
                    String errMsg = (itemsResponse != null) ? itemsResponse.getMessage() : "Mạng không phản hồi.";
                    System.err.println("⚠️ [Service Log] Server báo THẤT BẠI khi lấy sản phẩm. Message: " + errMsg);
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi trong luồng lấy Hot Auctions: " + e.getMessage());
            }
            return itemsResult;
        }, networkExecutor);

        // =========================================================================
        // ⚡ ĐIỂM GIAO THOA: Chờ cả 2 tác vụ chạy xong cùng lúc rồi gom dữ liệu trả về UI
        // =========================================================================
        CompletableFuture.allOf(statsTask, itemsTask)
                .thenAcceptAsync((v) -> {
                    try {
                        Map<String, Integer> finalStats = statsTask.join();
                        List<Item> finalItems = itemsTask.join();

                        System.out.println("✅ [Parallel-Net] Cả 2 luồng đã hoàn tất đồng thời! Đẩy dữ liệu về Callback cho Controller.");

                        // Kích hoạt callback để MainController tiến hành dựng giao diện (Render)
                        callback.accept(finalStats, finalItems);
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi nghiêm trọng khi đồng bộ hóa luồng callback dữ liệu: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, networkExecutor);
    }
}
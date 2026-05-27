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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class MainDashboardService {

    private static final ExecutorService networkExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r);
        t.setName("DashboardNetworkWorker");
        t.setDaemon(true);
        return t;
    });
     private static final AtomicBoolean isFetching = new AtomicBoolean(false);


    @SuppressWarnings("unchecked")
    public void fetchDashboardDataAsync(String accountId, String filter, BiConsumer<Map<String, Integer>, List<Item>> callback) {
        if (!isFetching.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                // Chạy tuần tự — tránh tranh connection DB
                Map<String, Integer> stats = null;
                try {
                    System.out.println("⚡ [Parallel-Net] Luồng 1 xuất phát bắn request: GET_DASHBOARD_STATS");
                    Response statsResponse = ClientSocket.getInstance().sendRequest(
                            new Request(MessageType.GET_DASHBOARD_STATS, accountId));
                    if (statsResponse != null && statsResponse.isSuccess()) {
                        stats = (Map<String, Integer>) statsResponse.getData();
                    }
                } catch (Exception e) {
                    System.err.println("❌ Lỗi Stats: " + e.getMessage());
                }

                List<Item> items = new ArrayList<>();
                try {
                    System.out.println("⚡ [Parallel-Net] Luồng 2 xuất phát bắn request: GET_HOT_AUCTIONS với Filter: " + filter);
                    Response itemsResponse = ClientSocket.getInstance().sendRequest(
                            new Request(MessageType.GET_HOT_AUCTIONS, new String[]{accountId, filter}));
                    if (itemsResponse != null && itemsResponse.isSuccess()) {
                        List<Item> dbItems = (List<Item>) itemsResponse.getData();
                        if (dbItems != null) items.addAll(dbItems);
                    } else {
                        String errMsg = (itemsResponse != null) ? itemsResponse.getMessage() : "Mạng không phản hồi.";
                        System.err.println("⚠️ [Service Log] Server báo THẤT BẠI khi lấy sản phẩm. Message: " + errMsg);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Lỗi Items: " + e.getMessage());
                }

                System.out.println("✅ [Parallel-Net] Cả 2 luồng đã hoàn tất đồng thời! Đẩy dữ liệu về Callback cho Controller.");
                callback.accept(stats, items);

            } finally {
                isFetching.set(false);            }
        }, networkExecutor);
    }
}
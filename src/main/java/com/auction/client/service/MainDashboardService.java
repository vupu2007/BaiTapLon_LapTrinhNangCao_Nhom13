package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.model.Item;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MainDashboardService {

    public void fetchDashboardDataAsync(String accountId, String filter, BiConsumer<Map<String, Integer>, List<Item>> callback) {
        Thread worker = new Thread(() -> {
            Map<String, Integer> statsResult = null;
            List<Item> itemsResult = new ArrayList<>();

            try {
                Request statsReq = new Request(MessageType.GET_DASHBOARD_STATS, accountId);
                Response statsResp = ClientSocket.getInstance().sendRequest(statsReq);
                if (statsResp != null && statsResp.isSuccess()) {
                    statsResult = (Map<String, Integer>) statsResp.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ [Service] Lỗi lấy stats: " + e.getMessage());
            }

            try {
                Request hotReq = new Request(MessageType.GET_HOT_AUCTIONS, filter);
                Response hotResp = ClientSocket.getInstance().sendRequest(hotReq);
                if (hotResp != null && hotResp.isSuccess() && hotResp.getData() != null) {
                    itemsResult.addAll((List<Item>) hotResp.getData());
                }
            } catch (Exception e) {
                System.err.println("❌ [Service] Lỗi lấy danh sách sản phẩm: " + e.getMessage());
            }

            final Map<String, Integer> finalStats = statsResult;
            final List<Item> finalItems = itemsResult;
            Platform.runLater(() -> callback.accept(finalStats, finalItems));

        }, "MainDashboardServiceWorker");

        worker.setDaemon(true);
        worker.start();
    }
}
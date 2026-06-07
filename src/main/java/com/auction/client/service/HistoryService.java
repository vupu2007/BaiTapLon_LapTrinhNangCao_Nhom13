package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Auction;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HistoryService {

    public void fetchHistoryStatsAsync(int userId, Consumer<Map<?, ?>> callback) {
        Thread worker = new Thread(() -> {
            Map<?, ?> result = null;
            try {
                Response resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.GET_BID_HISTORY_STATS, userId));
                if (resp != null && resp.isSuccess()) {
                    result = (Map<?, ?>) resp.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ [HistoryService] fetchHistoryStats: " + e.getMessage());
            }
            final Map<?, ?> finalResult = result;
            Platform.runLater(() -> callback.accept(finalResult));
        }, "HistoryStatsLoader");
        worker.setDaemon(true);
        worker.start();
    }

    public void fetchHistoryTableAsync(int userId, Consumer<List<Auction>> callback) {
        Thread worker = new Thread(() -> {
            List<Auction> result = null;
            try {
                Response resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.GET_AUCTIONS_BY_BIDDER, userId));
                if (resp != null && resp.isSuccess()) {
                    result = (List<Auction>) resp.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ [HistoryService] fetchHistoryTable: " + e.getMessage());
            }
            final List<Auction> finalResult = result;
            Platform.runLater(() -> callback.accept(finalResult));
        }, "HistoryTableLoader");
        worker.setDaemon(true);
        worker.start();
    }
}
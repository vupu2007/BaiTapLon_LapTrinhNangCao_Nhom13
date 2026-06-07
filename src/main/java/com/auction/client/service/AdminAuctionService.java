package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Auction;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import java.util.List;
import java.util.function.Consumer;

public class AdminAuctionService {

    public void fetchAllAuctionsAsync(Consumer<List<Auction>> callback) {
        Thread worker = new Thread(() -> {
            List<Auction> result = null;
            try {
                Response resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.GET_ALL_AUCTIONS, null));
                if (resp != null && resp.isSuccess()) {
                    result = (List<Auction>) resp.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ [AdminAuctionService] fetchAllAuctions: " + e.getMessage());
            }
            final List<Auction> finalResult = result;
            Platform.runLater(() -> callback.accept(finalResult));
        }, "AdminAuctionLoader");
        worker.setDaemon(true);
        worker.start();
    }

    public void cancelAuctionAsync(String auctionId, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            Response resp = null;
            try {
                String[] params = {auctionId, "CANCELED"};
                resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.UPDATE_AUCTION_STATUS, params));
            } catch (Exception e) {
                System.err.println("❌ [AdminAuctionService] cancelAuction: " + e.getMessage());
            }
            final Response finalResp = resp;
            Platform.runLater(() -> callback.accept(finalResp));
        }, "AdminAuctionCancelWorker");
        worker.setDaemon(true);
        worker.start();
    }
    public void fetchDashboardStatsAsync(Consumer<List<Auction>> callback) {
        Thread worker = new Thread(() -> {
            List<Auction> result = null;
            try {
                Response resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.GET_ALL_AUCTIONS, null));
                if (resp != null && resp.isSuccess()) {
                    result = (List<Auction>) resp.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ [AdminAuctionService] fetchDashboardStats: " + e.getMessage());
            }
            final List<Auction> finalResult = result;
            Platform.runLater(() -> callback.accept(finalResult));
        }, "AdminDashboardLoader");
        worker.setDaemon(true);
        worker.start();
    }
}
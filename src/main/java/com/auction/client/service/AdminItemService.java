package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdminItemService {

    public void fetchAllItemsAsync(Consumer<List<Map<String, String>>> callback) {
        Thread worker = new Thread(() -> {
            List<Map<String, String>> result = null;
            try {
                Response resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.GET_PRODUCTS, null));
                if (resp != null && resp.isSuccess()) {
                    result = (List<Map<String, String>>) resp.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ [AdminItemService] fetchAllItems: " + e.getMessage());
            }
            final List<Map<String, String>> finalResult = result;
            Platform.runLater(() -> callback.accept(finalResult));
        }, "AdminItemLoader");
        worker.setDaemon(true);
        worker.start();
    }

    public void deleteItemAsync(String itemId, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            Response resp = null;
            try {
                resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.DELETE_ITEM, new String[]{itemId}));
            } catch (Exception e) {
                System.err.println("❌ [AdminItemService] deleteItem: " + e.getMessage());
            }
            final Response finalResp = resp;
            Platform.runLater(() -> callback.accept(finalResp));
        }, "AdminItemDeleteWorker");
        worker.setDaemon(true);
        worker.start();
    }
}
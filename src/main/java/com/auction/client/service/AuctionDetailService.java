package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Auction;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.model.Item;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AuctionDetailService {

    /**
     * 🚀 Tải thông tin Item ngầm từ Server dựa vào ID vật phẩm
     */
    public void fetchItemByIdAsync(String itemId, Consumer<Item> callback) {
        Thread worker = new Thread(() -> {
            Item fetchedItem = null;
            try {
                Request itemReq = new Request(MessageType.GET_ITEM_BY_ID, itemId);
                Response itemResp = ClientSocket.getInstance().sendRequest(itemReq);
                if (itemResp != null && itemResp.isSuccess()) {
                    fetchedItem = (Item) itemResp.getData();
                }
            } catch (Exception ex) {
                System.err.println("❌ [Service] Lỗi lấy thông tin Item qua mạng: " + ex.getMessage());
            }

            final Item finalItem = fetchedItem;
            Platform.runLater(() -> callback.accept(finalItem));
        }, "AuctionDetailItemLoader");

        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 💰 CHUẨN ENTERPRISE: Gửi lệnh trả giá thực tế lên Server để lưu vào DB công khai
     */
    public void sendBidRequestAsync(int auctionId, int bidderId, double bidAmount, Consumer<Response> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                Object[] bidData = new Object[]{auctionId, bidderId, bidAmount};
                Response resp = ClientSocket.getInstance().sendRequest(new Request(MessageType.PLACE_BID, bidData));
                Platform.runLater(() -> callback.accept(resp));
            } catch (Exception ex) {
                Platform.runLater(() -> callback.accept(null));
            }
        });
    }
    public void fetchAuctionByItemIdAsync(String itemId, Consumer<Auction> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                Response response = ClientSocket.getInstance().sendRequest(
                        new Request(MessageType.GET_AUCTION_BY_ITEM_ID, itemId));
                if (response != null && response.isSuccess()) {
                    callback.accept((Auction) response.getData());
                } else {
                    callback.accept(null);
                }
            } catch (Exception e) {
                callback.accept(null);
            }
        });
    }
}
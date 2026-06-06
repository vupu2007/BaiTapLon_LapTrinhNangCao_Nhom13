package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item;
import com.auction.shared.model.Observer;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AuctionDetailService {

    // ==========================================
    // CÁC HÀM CŨ CỦA NHÓM BẠN (GIỮ NGUYÊN)
    // ==========================================

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
     * (Lưu ý: Hàm này trùng vai trò với hàm placeBid, bạn có thể dùng 1 trong 2 tùy thích)
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

    public void fetchBidHistoryAsync(int auctionId, Consumer<List<BidTransaction>> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                Response response = ClientSocket.getInstance().sendRequest(
                        new Request(MessageType.GET_BID_HISTORY, auctionId));
                if (response != null && response.isSuccess()) {
                    callback.accept((List<BidTransaction>) response.getData());
                } else {
                    callback.accept(null);
                }
            } catch (Exception e) {
                callback.accept(null);
            }
        });
    }

    // ==========================================
    // CÁC HÀM MỚI TÍCH HỢP (THEO ĐÚNG STYLE CODE CỦA BẠN)
    // ==========================================

    /**
     * 🌐 Đăng ký nhận tin Real-time từ phòng đấu giá
     */
    public void subscribeAuctionAsync(int auctionId, Observer observer, Consumer<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            boolean isSuccess = false;
            try {
                ClientSocket.getInstance().sendRequest(new Request(MessageType.SUBSCRIBE_AUCTION, auctionId));
                ClientSocket.getInstance().addAuctionObserver(auctionId, observer);
                isSuccess = true;
            } catch (Exception e) {
                System.err.println("❌ Lỗi đăng ký observer: " + e.getMessage());
            }

            final boolean finalSuccess = isSuccess;
            if (callback != null) {
                Platform.runLater(() -> callback.accept(finalSuccess));
            }
        });
    }

    /**
     * 🔒 Gửi yêu cầu kết thúc phiên đấu giá
     */
    public void closeAuctionAsync(int auctionId, Consumer<Response> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                Request request = new Request(MessageType.CLOSE_AUCTION, auctionId);
                Response response = ClientSocket.getInstance().sendRequest(request);
                if (callback != null) {
                    Platform.runLater(() -> callback.accept(response));
                }
            } catch (Exception e) {
                if (callback != null) {
                    Platform.runLater(() -> callback.accept(null));
                }
            }
        });
    }

    /**
     * 🧹 Hủy đăng ký lắng nghe phòng (Dọn dẹp khi thoát phòng)
     */
    public void unsubscribeAuctionAsync(int auctionId, Observer observer) {
        CompletableFuture.runAsync(() -> {
            try {
                Request req = new Request(MessageType.UNSUBSCRIBE_AUCTION, auctionId);
                ClientSocket.getInstance().sendRequest(req);
                ClientSocket.getInstance().removeAuctionObserver(auctionId, observer);
            } catch (Exception e) {
                System.err.println("❌ Lỗi hủy observer khi cleanup: " + e.getMessage());
            }
        });
    }
}
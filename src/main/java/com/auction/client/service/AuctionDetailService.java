package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.model.Item;
import javafx.application.Platform;
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
        Thread worker = new Thread(() -> {
            Response resp = null;
            try {
                // Đóng gói mảng dữ liệu hoặc object tùy thuộc vào kiến trúc Server của bạn nhận gì
                // Ở đây giả định gửi một mảng chứa thông tin phiên, người đặt, và số tiền nâng giá
                Object[] bidData = new Object[]{auctionId, bidderId, bidAmount};
                Request bidReq = new Request(MessageType.PLACE_BID, bidData);

                resp = ClientSocket.getInstance().sendRequest(bidReq);
            } catch (Exception ex) {
                System.err.println("❌ [Service] Lỗi gửi request đặt giá: " + ex.getMessage());
            }

            final Response finalResp = resp;
            Platform.runLater(() -> callback.accept(finalResp));
        }, "BidPlacementWorker");

        worker.setDaemon(true);
        worker.start();
    }
}
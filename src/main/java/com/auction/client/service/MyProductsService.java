package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.model.Item;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MyProductsService {

    /**
     * Tải danh sách sản phẩm của chủ sở hữu một cách bất đồng bộ
     */
    public void loadOwnerProductsAsync(int ownerId, Consumer<List<Item>> callback) {
        Thread worker = new Thread(() -> {
            List<Item> productList = null;
            try {
                Request req = new Request(MessageType.GET_ITEMS_BY_OWNER, ownerId);
                Response resp = ClientSocket.getInstance().sendRequest(req);
                if (resp != null && resp.isSuccess() && resp.getData() != null) {
                    productList = (List<Item>) resp.getData();
                }
            } catch (Exception ex) {
                System.err.println("❌ [Service] Lỗi gửi request lấy sản phẩm: " + ex.getMessage());
            }

            // Đồng bộ kết quả sạch trả về luồng giao diện FX
            final List<Item> finalResult = productList != null ? productList : new ArrayList<>();
            Platform.runLater(() -> callback.accept(finalResult));
        }, "OwnerProductsLoader");

        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Gửi yêu cầu xóa sản phẩm lên Server bất đồng bộ
     */
    public void deleteProductAsync(String itemId, Consumer<Boolean> callback) {
        Thread worker = new Thread(() -> {
            boolean isDeleted = false;
            try {
                Request delReq = new Request(MessageType.DELETE_ITEM, itemId);
                Response delResp = ClientSocket.getInstance().sendRequest(delReq);
                isDeleted = delResp != null && delResp.isSuccess();
            } catch (Exception e) {
                System.err.println("❌ [Service] Lỗi gửi request xóa sản phẩm: " + e.getMessage());
            }

            // Bắn kết quả True/False về cho Controller xử lý Alert hiển thị công khai
            final boolean finalStatus = isDeleted;
            Platform.runLater(() -> callback.accept(finalStatus));
        }, "ProductDeleteWorker");

        worker.setDaemon(true);
        worker.start();
    }
}
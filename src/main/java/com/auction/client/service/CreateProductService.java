package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Item;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class CreateProductService {

    /**
     * 🚀 PIPELINE LIÊN HOÀN BẤT ĐỒNG BỘ: Chuyển ảnh Base64 -> Tạo Vật phẩm -> Tạo phiên đấu giá ngầm
     */
    public void createAuctionPipelineAsync(
            String itemId, String name, String description, double startPrice, int ownerId,
            File imgFile, String startTimeStr, String endTimeStr, Consumer<Response> callback) {

        Thread worker = new Thread(() -> {
            try {
                // 1. Xử lý chuyển đổi file ảnh sang chuỗi Base64
                // 1. Xử lý chuyển đổi file ảnh sang chuỗi Base64
                String imagePathResult = null;
                if (imgFile != null && imgFile.exists()) {
                    try {
                        BufferedImage original = ImageIO.read(imgFile);
                        if (original != null) {
                            int maxW = 800, maxH = 600;
                            int w = original.getWidth(), h = original.getHeight();
                            if (w > maxW || h > maxH) {
                                double scale = Math.min((double)maxW/w, (double)maxH/h);
                                w = (int)(w * scale);
                                h = (int)(h * scale);
                                BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                                scaled.getGraphics().drawImage(
                                        original.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                                original = scaled;
                            }
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(original, "jpg", baos);
                            imagePathResult = "base64:" + Base64.getEncoder().encodeToString(baos.toByteArray());
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý ảnh: " + e.getMessage());
                    }
                }

                // 2. Tạo thực thể Vật phẩm (Electronics)
                Electronics newItem = new Electronics();
                newItem.setItemId(itemId);
                newItem.setName(name);
                newItem.setDescription(description);
                newItem.setStartingPrice(startPrice);
                newItem.setCategoryId(1);
                newItem.setOwnerId(ownerId);
                newItem.setStatus("AVAILABLE");
                newItem.setBrand("");
                newItem.setImagePath(imagePathResult);

                // Khởi tạo Request bằng Constructor 2 tham số chuẩn (type, payload) của nhóm bạn
                Request itemReq = new Request(MessageType.CREATE_ITEM, newItem);
                Response itemResp = ClientSocket.getInstance().sendRequest(itemReq);

                // Khởi tạo Response lỗi bằng Constructor 3 tham số chuẩn (boolean, String, Object) của nhóm bạn
                if (itemResp == null || !itemResp.isSuccess()) {
                    String msg = (itemResp != null) ? itemResp.getMessage() : "Không thể tạo vật phẩm (Mạng lỗi).";
                    Platform.runLater(() -> callback.accept(new Response(false, msg, null)));
                    return;
                }

                // 3. Nếu tạo vật phẩm thành công -> Kích hoạt tiếp lệnh tạo Phiên Đấu Giá (Auction)
                Object[] auctionData = {itemId, ownerId, startPrice, startTimeStr, endTimeStr};

                Request auctionReq = new Request(MessageType.CREATE_AUCTION, auctionData);
                Response auctionResp = ClientSocket.getInstance().sendRequest(auctionReq);

                // Gửi toàn bộ kết quả chốt hạ về cho Controller xử lý UI
                final Response finalResponse = auctionResp;
                Platform.runLater(() -> callback.accept(finalResponse));

            } catch (Exception e) {
                System.err.println("❌ [CreateProductService] Lỗi Pipeline: " + e.getMessage());
                // Đồng bộ constructor 3 tham số lỗi hệ thống tại đây
                Platform.runLater(() -> callback.accept(new Response(false, "Lỗi hệ thống: " + e.getMessage(), null)));
            }
        }, "AuctionCreationPipelineWorker");

        worker.setDaemon(true);
        worker.start();
    }
    public void updateItemAsync(Item item, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            try {
                System.out.println("DEBUG sending UPDATE_ITEM for " + item.getItemId());
                Request req = new Request(MessageType.UPDATE_ITEM, item);
                Response resp = ClientSocket.getInstance().sendRequest(req);
                Platform.runLater(() -> callback.accept(resp));
            } catch (Exception e) {
                System.out.println("DEBUG UPDATE_ITEM error: " + e.getMessage());
                Platform.runLater(() -> callback.accept(new Response(false, "Lỗi: " + e.getMessage(), null)));
            }
        }, "UpdateItemWorker");
        worker.setDaemon(true);
        worker.start();
    }
}
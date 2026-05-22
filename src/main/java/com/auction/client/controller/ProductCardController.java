package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    public void setData(String name, String price, String time, String imageFileName) {
        // 1. Gán các trường chữ trước (Chữ cực nhẹ, lên hình ngay lập tức không gây lag)
        productName.setText(name);
        currentPrice.setText(price);
        timeRemaining.setText(time);

        // Kiểm tra nếu tên file rỗng
        if (imageFileName == null || imageFileName.trim().isEmpty()) {
            imageFileName = "default.png";
        }

        final String finalImageFileName = imageFileName;

        // 2. 🚀 TỐI ƯU ĐA LUỒNG: Đẩy toàn bộ logic nạp và giải mã ảnh sang luồng ngầm (Background Thread)
        Thread imageLoadThread = new Thread(() -> {
            try {
                Image img;
                if (finalImageFileName.startsWith("http://") || finalImageFileName.startsWith("https://")) {
                    img = new Image(finalImageFileName, true); // URL online bản thân nó đã hỗ trợ background load
                }
                // Xử lý giải mã ảnh Base64 từ Database
                else if (finalImageFileName.startsWith("base64:")) {
                    String base64Data = finalImageFileName.substring(7);
                    // Công đoạn tốn CPU nhất: Giải mã mảng byte (Chạy ngầm nên không lo lag app)
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    img = new Image(new ByteArrayInputStream(imageBytes));
                }
                // Xử lý nạp ảnh tĩnh local
                else {
                    String localPath = "/com/auction/client/images/" + finalImageFileName;
                    var stream = getClass().getResourceAsStream(localPath);
                    if (stream == null) throw new Exception("Không tìm thấy file: " + localPath);
                    img = new Image(stream);
                }

                // 🌟 SAU KHI DỊCH ẢNH XONG: Dùng Platform.runLater để thảy ảnh lên giao diện an toàn
                final Image finalImg = img;
                Platform.runLater(() -> {
                    if (productImage != null) {
                        productImage.setImage(finalImg);
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Lỗi load ảnh ngầm cho sản phẩm [" + name + "]: " + e.getMessage());
                // Nếu lỗi, nạp ảnh fallback mặc định an toàn
                Platform.runLater(() -> {
                    if (productImage != null) {
                        var fallback = getClass().getResourceAsStream("/com/auction/client/images/default.png");
                        productImage.setImage(fallback != null ? new Image(fallback) : null);
                    }
                });
            }
        });

        // Thiết lập Daemon = true để luồng tự hủy khi tắt ứng dụng, tránh rò rỉ bộ nhớ (Memory Leak)
        imageLoadThread.setDaemon(true);
        // Kích hoạt luồng chạy ngầm hoạt động
        imageLoadThread.start();

        // Xử lý sự kiện bấm nút xem chi tiết
        actionButton.setOnAction(e -> {
            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().openAuctionDetail(name, price);
            }
        });
    }
}
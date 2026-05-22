package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    // ✅ ĐÃ NÂNG CẤP: Nhận tên file ảnh động từ Database truyền sang
    public void setData(String name, String price, String time, String imageFileName) {
        System.out.println("🖼️ setData called — name=" + name + " | image=" + imageFileName);
        productName.setText(name);
        currentPrice.setText(price);
        timeRemaining.setText(time);

        // Load ảnh
        if (imageFileName == null || imageFileName.trim().isEmpty()) {
            imageFileName = "default.png";
        }

        try {
            Image img;
            if (imageFileName.startsWith("http://") || imageFileName.startsWith("https://")) {
                img = new Image(imageFileName, true); // ✅ URL online
            } else {
                String localPath = "/com/auction/client/images/" + imageFileName;
                var stream = getClass().getResourceAsStream(localPath);
                if (stream == null) throw new Exception("Không tìm thấy file: " + localPath);
                img = new Image(stream); // ✅ dùng localPath
            }
            productImage.setImage(img);

        } catch (Exception e) {
            System.err.println("❌ Lỗi load ảnh: " + e.getMessage());
            var fallback = getClass().getResourceAsStream("/com/auction/client/images/default.png");
            productImage.setImage(fallback != null ? new Image(fallback) : null);
        }

        actionButton.setOnAction(e -> {
            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().openAuctionDetail(name, price);
            }
        });
    }
}
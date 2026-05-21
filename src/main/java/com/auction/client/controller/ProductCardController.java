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
        this.productName.setText(name);
        this.currentPrice.setText(price);
        this.timeRemaining.setText(time);

        // Nếu trong DB cột ảnh bị null hoặc trống, tự động dùng ảnh mặc định
        if (imageFileName == null || imageFileName.trim().isEmpty()) {
            imageFileName = "default.png";
        }

        // Tạo đường dẫn động quét trong thư mục resources
        String imagePath = "/com/auction/client/images/" + imageFileName;

        try {
            Image img = new Image(getClass().getResourceAsStream(imagePath));
            productImage.setImage(img);
        } catch (Exception e) {
            System.err.println("❌ Không tìm thấy file ảnh: " + imageFileName + ", đổi về ảnh mặc định.");
            try {
                productImage.setImage(new Image(getClass().getResourceAsStream("/com/auction/client/images/default.png")));
            } catch (Exception ex) {
                productImage.setImage(null);
            }
        }

        // Logic xử lý nút bấm giữ nguyên
        actionButton.setOnAction(e -> {
            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().openAuctionDetail(name, price);
            }
        });
    }
}
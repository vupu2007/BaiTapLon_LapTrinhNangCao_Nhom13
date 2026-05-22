package com.auction.client.controller;

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

    // ✅ ĐÃ NÂNG CẤP: Hỗ trợ giải mã ảnh Base64 đồng bộ từ Cloud
    public void setData(String name, String price, String time, String imageFileName) {
        System.out.println("🖼️ setData called — name=" + name + " | image=" + (imageFileName != null && imageFileName.startsWith("base64:") ? "[Chuỗi Base64]" : imageFileName));
        productName.setText(name);
        currentPrice.setText(price);
        timeRemaining.setText(time);

        // Kiểm tra nếu tên file rỗng
        if (imageFileName == null || imageFileName.trim().isEmpty()) {
            imageFileName = "default.png";
        }

        try {
            Image img;
            if (imageFileName.startsWith("http://") || imageFileName.startsWith("https://")) {
                img = new Image(imageFileName, true); // ✅ URL online
            }
            // 🚀 THÊM NHÁNH NÀY: Xử lý giải mã ảnh Base64 từ Database Cloud
            else if (imageFileName.startsWith("base64:")) {
                // Tách bỏ chữ "base64:" ở đầu để lấy chuỗi mã hóa gốc
                String base64Data = imageFileName.substring(7);
                // Giải mã chuỗi text thành mảng byte dữ liệu ảnh
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                // Nạp mảng byte vào stream để tạo đối tượng Image
                img = new Image(new ByteArrayInputStream(imageBytes));
            }
            else {
                // Xử lý nạp ảnh tĩnh local như cũ
                String localPath = "/com/auction/client/images/" + imageFileName;
                var stream = getClass().getResourceAsStream(localPath);
                if (stream == null) throw new Exception("Không tìm thấy file: " + localPath);
                img = new Image(stream);
            }
            productImage.setImage(img);

        } catch (Exception e) {
            System.err.println("❌ Lỗi load ảnh cho sản phẩm [" + name + "]: " + e.getMessage());
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
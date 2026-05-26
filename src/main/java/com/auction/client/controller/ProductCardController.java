package com.auction.client.controller;

import com.auction.client.util.ImageLoader; // 🚀 Gọi class tiện ích tập trung của hệ thống
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    @FXML
    public void initialize() {
        // Khởi tạo khuôn cắt ảnh bo góc khớp chuẩn với ImageView trong FXML
        if (productImage != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(productImage.fitWidthProperty());
            clip.heightProperty().bind(productImage.fitHeightProperty());
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            productImage.setClip(clip);
        }
    }

    public void setSellerMode(Runnable onEditAction, Runnable onDeleteAction) {
        if (actionButton != null) {
            actionButton.setVisible(false);
            actionButton.setManaged(false);
        }

        if (btnEdit != null && btnDelete != null) {
            btnEdit.setVisible(true);
            btnEdit.setManaged(true);
            btnDelete.setVisible(true);
            btnDelete.setManaged(true);

            btnEdit.setOnAction(e -> { if (onEditAction != null) onEditAction.run(); });
            btnDelete.setOnAction(e -> { if (onDeleteAction != null) onDeleteAction.run(); });
        }
    }

    public void setData(String name, String price, String statusText, String imageFileName,
                        String description, String sellerName, String startTime, String endTime) {

        // 🌟 ĐỒNG BỘ DỮ LIỆU CHỮ VỚI FXML
        if (productName != null) productName.setText(name);
        if (currentPrice != null) currentPrice.setText(price);
        if (productDesc != null) productDesc.setText(description);

        // Gán text trạng thái (Đang diễn ra / Sắp diễn ra)
        if (statusBadge != null) {
            statusBadge.setText(statusText);
            if ("Sắp diễn ra".equals(statusText)) {
                statusBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-background-radius: 20; -fx-font-weight: bold;");
            } else {
                statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 20; -fx-font-weight: bold;");
            }
        }

        if (timeRemaining != null) {
            timeRemaining.setText("Sắp diễn ra".equals(statusText) ? "--:--:--" : "02:45:10");
        }

        // 🚀 CHUẨN KIẾN TRÚC LỚN: ỦY THÁC TOÀN BỘ VIỆC LOAD ẢNH + CACHE CHO IMAGELOADER
        // Hàm này tự chạy ngầm, tự check Cache Caffeine trong RAM, tự đổi ảnh default nếu lỗi
        ImageLoader.tryLoadImageToView(productImage, imageFileName);

        // Xử lý sự kiện click vào nút Đấu giá ngay / Xem chi tiết
        if (actionButton != null) {
            actionButton.setText("Sắp diễn ra".equals(statusText) ? "Xem chi tiết" : "Đấu giá ngay");
            actionButton.setOnAction(e -> {
                if (MainLayoutController.getInstance() != null) {
                    Image currentImg = (productImage != null) ? productImage.getImage() : null;

                    MainLayoutController.getInstance().openAuctionDetail(
                            name,
                            price,
                            currentImg,
                            imageFileName,
                            description,
                            sellerName,
                            startTime,
                            endTime
                    );
                }
            });
        }
    }
}
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
    @FXML private Button actionButton; // Nút mặc định (Đấu giá ngay)

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

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

    public void setData(String name, String price, String time, String imageFileName) {
        productName.setText(name);
        currentPrice.setText(price);
        timeRemaining.setText(time);

        if (imageFileName == null || imageFileName.trim().isEmpty()) {
            imageFileName = "default.png";
        }

        final String finalImageFileName = imageFileName;

        // Tải ảnh đa luồng ngầm
        Thread imageLoadThread = new Thread(() -> {
            try {
                Image img;
                if (finalImageFileName.startsWith("http://") || finalImageFileName.startsWith("https://")) {
                    img = new Image(finalImageFileName, true);
                }
                else if (finalImageFileName.startsWith("base64:")) {
                    String base64Data = finalImageFileName.substring(7);
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    img = new Image(new ByteArrayInputStream(imageBytes));
                }
                else {
                    String localPath = "/com/auction/client/images/" + finalImageFileName;
                    var stream = getClass().getResourceAsStream(localPath);
                    if (stream == null) throw new Exception("Không tìm thấy file: " + localPath);
                    img = new Image(stream);
                }

                final Image finalImg = img;
                Platform.runLater(() -> {
                    if (productImage != null) {
                        productImage.setImage(finalImg);

                        // 🔥 ĐÃ SỬA: Tạo khuôn cắt ĐỘNG tự co giãn chuẩn khít theo ImageView thực tế
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                        clip.widthProperty().bind(productImage.fitWidthProperty());
                        clip.heightProperty().bind(productImage.fitHeightProperty());

                        // Đường kính bo góc là 24 (Tương đương bán kính radius 12px chuẩn trong file FXML)
                        clip.setArcWidth(24);
                        clip.setArcHeight(24);

                        productImage.setClip(clip);
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Lỗi load ảnh sản phẩm [" + name + "]: " + e.getMessage());
                Platform.runLater(() -> {
                    if (productImage != null) {
                        var fallback = getClass().getResourceAsStream("/com/auction/client/images/default.png");
                        productImage.setImage(fallback != null ? new Image(fallback) : null);

                        // 🔥 ĐÃ SỬA: Áp dụng khuôn cắt động tương tự cho ảnh mặc định khi lỗi
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                        clip.widthProperty().bind(productImage.fitWidthProperty());
                        clip.heightProperty().bind(productImage.fitHeightProperty());
                        clip.setArcWidth(24);
                        clip.setArcHeight(24);

                        productImage.setClip(clip);
                    }
                });
            }
        });

        imageLoadThread.setDaemon(true);
        imageLoadThread.start();

        // 🔥 SỬA TẠI ĐÂY: Truyền cả Object ảnh trên UI kèm theo Chuỗi dữ liệu ảnh gốc (đề phòng ảnh UI chưa load xong)
        actionButton.setOnAction(e -> {
            if (MainLayoutController.getInstance() != null) {
                Image currentImg = (productImage != null) ? productImage.getImage() : null;
                MainLayoutController.getInstance().openAuctionDetail(name, price, currentImg, finalImageFileName);
            }
        });
    }
}
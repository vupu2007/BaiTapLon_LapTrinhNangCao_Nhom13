package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    // 🌟 TỐI ƯU: Sử dụng Thread Pool cố định để tái sử dụng luồng, tránh vắt kiệt CPU máy khách
    private static final ExecutorService imageExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
    );

    @FXML
    public void initialize() {
        // 🌟 CRITICAL FIX: Khởi tạo khuôn cắt ảnh BO GÓC duy nhất một lần tại đây để chống tràn bộ nhớ (Memory Leak)
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

    public void setData(String name, String price, String time, String imageFileName,
                        String description, String sellerName, String startTime, String endTime) {

        // 🌟 PHÒNG VỆ: Kiểm tra Null-Pointer an toàn cho tất cả các thành phần FXML
        if (productName != null) productName.setText(name);
        if (currentPrice != null) currentPrice.setText(price);
        if (timeRemaining != null) timeRemaining.setText(time);
        if (productDesc != null) productDesc.setText(description);

        if (imageFileName == null || imageFileName.trim().isEmpty()) {
            imageFileName = "default.png";
        }

        final String finalImageFileName = imageFileName;

        // 🌟 🚀 CẢI TIẾN HIỆU NĂNG: Tận dụng cơ chế chạy ngầm thông minh tùy thuộc vào loại dữ liệu đầu vào
        if (finalImageFileName.startsWith("http://") || finalImageFileName.startsWith("https://")) {
            // Chuẩn JavaFX: Tham số thứ hai là 'backgroundLoading = true' giúp tự tải ngầm không cần tạo Thread thô!
            Image webImg = new Image(finalImageFileName, true);
            if (productImage != null) productImage.setImage(webImg);
        }
        else {
            // Đẩy các tác vụ giải mã Base64 nặng hoặc đọc ổ đĩa cục bộ vào Pool quản lý luồng tập trung
            imageExecutor.submit(() -> {
                try {
                    Image img;
                    if (finalImageFileName.startsWith("base64:")) {
                        String base64Data = finalImageFileName.substring(7);
                        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                        img = new Image(new ByteArrayInputStream(imageBytes));
                    }
                    else {
                        // Thêm quét đường dẫn linh hoạt ngăn lỗi cấu trúc thư mục tài nguyên
                        String localPath = "/com/auction/client/images/" + finalImageFileName;
                        var stream = getClass().getResourceAsStream(localPath);
                        if (stream == null) {
                            stream = getClass().getResourceAsStream("/images/" + finalImageFileName);
                        }
                        if (stream == null) throw new Exception("Không tìm thấy file ảnh cục bộ: " + finalImageFileName);
                        img = new Image(stream);
                    }

                    final Image finalImg = img;
                    Platform.runLater(() -> {
                        if (productImage != null) productImage.setImage(finalImg);
                    });

                } catch (Exception e) {
                    System.err.println("❌ Lỗi load ảnh sản phẩm [" + name + "]: " + e.getMessage());
                    Platform.runLater(() -> {
                        if (productImage != null) {
                            var fallback = getClass().getResourceAsStream("/com/auction/client/images/default.png");
                            if (fallback == null) fallback = getClass().getResourceAsStream("/images/default.png");
                            productImage.setImage(fallback != null ? new Image(fallback) : null);
                        }
                    });
                }
            });
        }

        if (actionButton != null) {
            actionButton.setOnAction(e -> {
                if (MainLayoutController.getInstance() != null) {
                    Image currentImg = (productImage != null) ? productImage.getImage() : null;

                    MainLayoutController.getInstance().openAuctionDetail(
                            name,
                            price,
                            currentImg,
                            finalImageFileName,
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
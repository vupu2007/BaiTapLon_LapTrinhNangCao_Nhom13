package com.auction.client.controller;

import com.auction.client.util.ImageLoader; // 🚀 Gọi class tiện ích tập trung của hệ thống
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    // Giữ lại Object gốc của thẻ sản phẩm phục vụ cho việc truyền nhận trang chi tiết động
    private Object originProductData;

    @FXML private Label lblTimeRemainingTitle;

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

    /**
     * Hàm nạp dữ liệu cũ: Hỗ trợ tương thích ngược với các hàm gọi cũ chỉ truyền Text chuỗi thô
     */
    public void setData(String name, String price, String statusText, String imageFileName,
                        String description, String sellerName, String startTime, String endTime) {
        setDataInternal(name, price, statusText, imageFileName, description,
                "Sắp diễn ra".equals(statusText) ? startTime : endTime);
    }

    /**
     * 🌟 HÀM NÂNG CAO: Nạp dữ liệu đồng thời gán dữ liệu Model Object gốc (Item hoặc Auction)
     * Giúp hệ thống không bao giờ phải chạy vào khối logic dự phòng (Fallback) khi đổi trang
     */
    public void setProductModelData(Object originModel, String name, String price, String statusText, String imageFileName, String description, String startTimeStr, String endTimeStr) {
        this.originProductData = originModel;
        setDataInternal(name, price, statusText, imageFileName, description,
                "Sắp diễn ra".equals(statusText) ? startTimeStr : endTimeStr);
    }
    private void setDataInternal(String name, String price, String statusText, String imageFileName, String description, String endTimeStr) {
        // 1. Chỉ set dữ liệu (Màu sắc đã được file FXML gán cứng bảo vệ)
        if (productName != null) productName.setText(name);
        if (currentPrice != null) currentPrice.setText(price);
        if (productDesc != null) productDesc.setText(description);

        // 2. Logic động cho Badge trạng thái (Phải giữ vì màu nền thay đổi liên tục theo data)
        if (statusBadge != null) {
            statusBadge.setText(statusText);
            if ("Sắp diễn ra".equals(statusText)) {
                statusBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-background-radius: 20; -fx-font-weight: bold;");
            } else if ("Đã kết thúc".equals(statusText) || "Sắp kết thúc".equals(statusText)) {
                statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-background-radius: 20; -fx-font-weight: bold;");
            } else if ("Bị hủy".equals(statusText)) {
            statusBadge.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04; -fx-background-radius: 20; -fx-font-weight: bold;");
        }
        else {
                statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 20; -fx-font-weight: bold;");
            }
        }

        // 3. Tiêu đề thời gian
        if (lblTimeRemainingTitle != null) {
            lblTimeRemainingTitle.setText("Sắp diễn ra".equals(statusText) ? "Bắt đầu sau:" : "Còn lại:");
        }

        // 4. Tính toán thời gian đếm ngược
        if (timeRemaining != null) {
            try {
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTimeStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                java.time.Duration d = java.time.Duration.between(java.time.LocalDateTime.now(), end);
                timeRemaining.setText(d.isNegative() ? "Đã kết thúc" : String.format("%02dh %02dm", d.toHours(), d.toMinutesPart()));
            } catch (Exception e) {
                timeRemaining.setText("--:--");
            }
        }

        // 5. Load ảnh sản phẩm và nút bấm hành động
        if (imageFileName != null && imageFileName.startsWith("base64:")) {
            try {
                // Bóc chữ "base64:" ra và giải mã
                String base64Data = imageFileName.substring(7);
                byte[] imgBytes = java.util.Base64.getDecoder().decode(base64Data);
                productImage.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imgBytes)));
            } catch (Exception ex) {
                System.err.println("❌ Lỗi giải mã ảnh Base64 trên Card: " + ex.getMessage());
            }
        } else {
            ImageLoader.tryLoadImageToView(productImage, imageFileName);
        }
        if (actionButton != null) {
            actionButton.setText("Sắp diễn ra".equals(statusText) || "Đã kết thúc".equals(statusText) ? "Xem chi tiết" : "Đấu giá ngay");
            actionButton.setOnAction(e -> {
                // Kiểm tra xem nút bấm có nằm trong Card không
                if (actionButton.getParent() != null) {
                    // Lội ngược lên kích hoạt sự kiện MOUSE_CLICKED của Card bọc bên ngoài
                    actionButton.getParent().fireEvent(new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                            javafx.scene.input.MouseButton.PRIMARY, 1,
                            false, false, false, false, true, false, false, true, false, false, null
                    ));
                }
            });
        }
    }
    /**
     * 🚀 SỬA LỖI BIÊN DỊCH TRIỆT ĐỂ: Tự điều hướng sang trang chi tiết bằng kỹ thuật Scene Graph Lookup
     * Loại bỏ hoàn toàn cơ chế gọi qua Singleton static cũ và sửa lỗi khởi tạo lớp abstract Item.
     */
    private void handleNavigateToDetail() {
        if (actionButton.getScene() == null) return;

        // 🎯 FIX: Sử dụng Auction (Concrete Class) làm phương án dự phòng thay vì khởi tạo lớp trừu tượng Item
        if (originProductData == null) {
            Auction fallbackAuction = new Auction();
            fallbackAuction.setProductName(productName != null ? productName.getText() : "Sản phẩm");
            try {
                String rawPrice = currentPrice != null ? currentPrice.getText().replaceAll("[^0-9]", "") : "0";
                fallbackAuction.setStartPrice(Double.parseDouble(rawPrice));
                fallbackAuction.setCurrentPrice(Double.parseDouble(rawPrice));
            } catch (Exception ex) {
                fallbackAuction.setStartPrice(0.0);
                fallbackAuction.setCurrentPrice(0.0);
            }
            if (productDesc != null) {
                fallbackAuction.setItemId("FALLBACK-" + productName.getText().hashCode());
            }
            this.originProductData = fallbackAuction;
        }

        // Tạo tiến trình chạy nền nạp FXML trang chi tiết, giữ nút click phản hồi tức thì
        Thread navigationWorker = new Thread(() -> {
            try {
                String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                        ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";

                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent detailView = loader.load();

                // Đổ Object dữ liệu nguyên bản trực tiếp vào Controller trang chi tiết mới
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    if (originProductData instanceof Item item) detailController.loadProductDetail(item);
                    else if (originProductData instanceof Auction auction) detailController.loadProductDetail(auction);
                }

                // Cập nhật giao diện đè lên khung contentArea của Layout cha chính thức
                Platform.runLater(() -> {
                    Parent root = actionButton.getScene().getRoot();
                    Node layoutCenter = root.lookup("#contentArea");

                    if (layoutCenter instanceof StackPane contentArea) {
                        contentArea.getChildren().setAll(detailView);
                        System.out.println("🎯 [Navigation] Card sản phẩm chuyển tiếp sang trang chi tiết thành công.");
                    } else {
                        System.err.println("❌ Không thể định vị được vùng hiển thị #contentArea của bố cục cha.");
                    }
                });

            } catch (Exception ex) {
                System.err.println("❌ Lỗi chuyển hướng trang chi tiết từ ProductCard: " + ex.getMessage());
            }
        });
        navigationWorker.setDaemon(true);
        navigationWorker.start();
    }
    public void setOriginProductData(Object data) {
        this.originProductData = data;
    }

}
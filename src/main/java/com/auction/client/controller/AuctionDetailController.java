package com.auction.client.controller;

import com.auction.shared.model.Item;
import com.auction.shared.model.Auction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.time.format.DateTimeFormatter;

public class AuctionDetailController {

    @FXML
    public Label lblProductTitle, lblTimeRemaining, lblInfoName, lblInfoDescription,
            lblStartPrice, lblSellerName, lblStartTime, lblEndTime,
            lblCurrentPrice, lblTopBidder;

    @FXML
    private ImageView imgProduct;
    @FXML
    private LineChart<Number, Number> chartPriceHistory;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Button btnSubmitBid;
    @FXML
    private ToggleButton btnAutoBid;
    @FXML
    private VBox vboxBidHistoryContainer;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (btnSubmitBid != null) {
            btnSubmitBid.setOnAction(event -> handleBid());
        }
        if (btnAutoBid != null) {
            btnAutoBid.selectedProperty().addListener((obs, oldVal, newVal) -> {
                btnAutoBid.setText(newVal ? "Bật" : "Tắt");
            });
        }
    }

    /**
     * 🔥 HÀM NẠP DỮ LIỆU CHUẨN ĐỒNG BỘ ĐỦ 8 THAM SỐ VỚI MAIN_LAYOUT
     */
    public void initData(String productName, String startingPrice, javafx.scene.image.Image fxImage, String imageFileName,
                         String description, String sellerName, String startTime, String endTime) {

        // 1. Gán thông tin chữ cơ bản
        if (lblProductTitle != null) lblProductTitle.setText(productName);
        if (lblInfoName != null) lblInfoName.setText(productName);
        if (lblInfoDescription != null) lblInfoDescription.setText(description);
        if (lblSellerName != null) lblSellerName.setText(sellerName);

        // 2. Hiển thị giá tiền
        if (lblStartPrice != null) {
            lblStartPrice.setText(startingPrice);
        }

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(startingPrice);
        }

        // 3. Cơ chế kiểm tra chống ghi đè thời gian trống (Giữ lại giờ cũ nếu chuỗi truyền sang không hợp lệ)
        if (lblStartTime != null) {
            if (startTime != null && !startTime.trim().isEmpty() && !startTime.contains("--/--")) {
                lblStartTime.setText(startTime);
            }
        }

        if (lblEndTime != null) {
            if (endTime != null && !endTime.trim().isEmpty() && !endTime.contains("--/--")) {
                lblEndTime.setText(endTime);
            }
        }

        // 4. Xử lý hiển thị ảnh an toàn thông minh
        if (imgProduct != null) {
            if (fxImage != null) {
                imgProduct.setImage(fxImage);
            } else if (imageFileName != null && !imageFileName.trim().isEmpty() && !imageFileName.equals("null")) {
                tryLoadImageToView(imgProduct, imageFileName);
            } else {
                loadFallbackDefaultImage();
            }
        }
    }

    /**
     * 🖼️ Tự động tải ảnh thông minh từ upload ngoài hoặc tài nguyên hệ thống
     */
    private void tryLoadImageToView(ImageView imgView, Object imageSource) {
        if (imgView == null || imageSource == null) {
            loadFallbackDefaultImage();
            return;
        }

        if (imageSource instanceof javafx.scene.image.Image) {
            imgView.setImage((javafx.scene.image.Image) imageSource);
            return;
        }

        if (imageSource instanceof String preferredFileName) {
            try {
                if (preferredFileName.startsWith("base64:")) {
                    byte[] bytes = Base64.getDecoder().decode(preferredFileName.substring(7));
                    imgView.setImage(new Image(new ByteArrayInputStream(bytes)));
                    return;
                }
                if (preferredFileName.startsWith("http://") || preferredFileName.startsWith("https://")) {
                    imgView.setImage(new Image(preferredFileName, true));
                    return;
                }

                File file = new File("C:/uet_uploads/" + preferredFileName);
                if (file.exists() && file.isFile()) {
                    imgView.setImage(new Image(file.toURI().toString()));
                    return;
                }

                InputStream is = getClass().getResourceAsStream("/com/auction/client/images/" + preferredFileName);
                if (is != null) {
                    imgView.setImage(new Image(is));
                } else {
                    loadFallbackDefaultImage();
                }
            } catch (Exception e) {
                loadFallbackDefaultImage();
            }
        } else {
            loadFallbackDefaultImage();
        }
    }

    private void loadFallbackDefaultImage() {
        try {
            InputStream is = getClass().getResourceAsStream("/com/auction/client/images/default.png");
            if (is == null) is = getClass().getResourceAsStream("/view/images/default.png");
            if (is == null) is = getClass().getResourceAsStream("/images/default.png");

            if (is != null) {
                imgProduct.setImage(new Image(is));
            } else {
                imgProduct.setImage(null);
            }
        } catch (Exception ex) {
            imgProduct.setImage(null);
        }
    }

    /**
     * 🚀 HÀM 1: Nhận đối tượng Item thô từ trang chủ
     */
    public void loadProductDetail(Item item) {
        if (item == null) return;

        String formattedPrice = String.format("%,.0f đ", item.getStartingPrice());
        String sellerName = "Người bán #" + item.getOwnerId();

        if (com.auction.client.util.CurrentAccount.getAccount() != null) {
            String itemOwnerIdStr = String.valueOf(item.getOwnerId());
            String currentUserIdStr = com.auction.client.util.CurrentAccount.getAccount().getId();

            if (currentUserIdStr != null && (currentUserIdStr.equals(itemOwnerIdStr) || item.getOwnerId() == 1)) {
                sellerName = com.auction.client.util.CurrentAccount.getAccount().getUsername();
            }
        }

        String startTimeStr = "--/--/---- --:--";
        String endTimeStr = "--/--/---- --:--";
        try {
            java.lang.reflect.Method getStartMethod = item.getClass().getMethod("getStartTime");
            Object sTime = getStartMethod.invoke(item);
            if (sTime instanceof java.time.LocalDateTime) {
                startTimeStr = ((java.time.LocalDateTime) sTime).format(dateTimeFormatter);
            } else if (sTime != null) {
                startTimeStr = sTime.toString();
            }
        } catch (Exception e) {}

        try {
            java.lang.reflect.Method getEndMethod = item.getClass().getMethod("getEndTime");
            Object eTime = getEndMethod.invoke(item);
            if (eTime instanceof java.time.LocalDateTime) {
                endTimeStr = ((java.time.LocalDateTime) eTime).format(dateTimeFormatter);
            } else if (eTime != null) {
                endTimeStr = eTime.toString();
            }
        } catch (Exception e) {}

        // Đẩy chuẩn xác qua hàm initData 8 tham số
        initData(item.getName(), formattedPrice, null, item.getImagePath(),
                item.getDescription() != null ? item.getDescription() : "Không có mô tả.",
                sellerName, startTimeStr, endTimeStr);
    }

    /**
     * 🚀 HÀM 2: Nhận đối tượng Auction từ Realtime / DB (ĐÃ XÓA BỎ HOÀN TOÀN GETITEM LỖI)
     */
    public void loadProductDetail(Auction auction) {
        if (auction == null) return;

        String pName = (auction.getProductName() != null) ? auction.getProductName() : "Mã sản phẩm #" + auction.getItemId();
        String startPriceStr = String.format("%,.0f đ", auction.getStartPrice());

        double currentPriceVal = auction.getCurrentPrice() > 0 ? auction.getCurrentPrice() : auction.getStartPrice();
        String currentPriceStr = String.format("%,.0f đ", currentPriceVal);

        String sellerName = "Người bán #" + auction.getSellerId();
        if (auction.getAccount() != null && auction.getAccount().getUsername() != null) {
            sellerName = auction.getAccount().getUsername();
        } else if (com.auction.client.util.CurrentAccount.getAccount() != null) {
            String auctionSellerIdStr = String.valueOf(auction.getSellerId());
            String currentUserIdStr = com.auction.client.util.CurrentAccount.getAccount().getId();
            if (currentUserIdStr != null && currentUserIdStr.equals(auctionSellerIdStr)) {
                sellerName = com.auction.client.util.CurrentAccount.getAccount().getUsername();
            }
        }

        // 🕰️ Giữ lại dữ liệu giờ cũ trên UI nếu object cập nhật ngầm bị thiếu dữ liệu thời gian
        String startTimeStr = (lblStartTime != null && lblStartTime.getText() != null && !lblStartTime.getText().contains("-"))
                ? lblStartTime.getText() : "--/--/---- --:--";
        String endTimeStr = (lblEndTime != null && lblEndTime.getText() != null && !lblEndTime.getText().contains("-"))
                ? lblEndTime.getText() : "--/--/---- --:--";

        if (auction.getStartTime() != null) {
            startTimeStr = auction.getStartTime().format(dateTimeFormatter);
        }
        if (auction.getEndTime() != null) {
            endTimeStr = auction.getEndTime().format(dateTimeFormatter);
        }

        String statusStr = auction.getStatus() != null ? auction.getStatus().name() : "RUNNING";

        Object imgObj = "default.png";
        try {
            java.lang.reflect.Method getImgMethod = auction.getClass().getMethod("getImage");
            Object res = getImgMethod.invoke(auction);
            if (res != null) imgObj = res;
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getImgUrlMethod = auction.getClass().getMethod("getImageUrl");
                Object res = getImgUrlMethod.invoke(auction);
                if (res != null) imgObj = res;
            } catch (Exception ex) {
                imgObj = (auction.getItemId() != null) ? auction.getItemId() : "default.png";
            }
        }

        // ĐỒNG BỘ KHỚP THAM SỐ: Đưa ảnh về dạng String an toàn nếu có thể, truyền đi đồng bộ
        initData(pName, startPriceStr, null, (imgObj instanceof String ? (String)imgObj : null),
                "Trạng thái phiên: " + statusStr, sellerName, startTimeStr, endTimeStr);

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(currentPriceStr);
        }

        // Nạp ảnh bằng cơ chế Object kiểm tra đa năng độc lập để không làm mất ảnh base64/file
        tryLoadImageToView(imgProduct, imgObj);
    }

    public void loadProductDetail(com.auction.shared.model.Auction auction, String fallbackImg) {
        loadProductDetail(auction);
    }

    private void handleBid() {
        if (txtBidAmount != null) {
            System.out.println("Đang tiến hành đặt giá: " + txtBidAmount.getText());
        }
    }

    @FXML
    private void handleBack() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().openHome();
        }
    }
}
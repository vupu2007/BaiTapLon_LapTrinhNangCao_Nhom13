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
import java.util.Base64;

public class AuctionDetailController {

    @FXML public Label lblProductTitle, lblTimeRemaining, lblInfoName, lblInfoDescription,
            lblStartPrice, lblSellerName, lblStartTime, lblEndTime,
            lblCurrentPrice, lblTopBidder;

    @FXML private ImageView imgProduct;
    @FXML private LineChart<Number, Number> chartPriceHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;
    @FXML private ToggleButton btnAutoBid;
    @FXML private VBox vboxBidHistoryContainer;

    @FXML
    public void initialize() {
        btnSubmitBid.setOnAction(event -> handleBid());
        btnAutoBid.selectedProperty().addListener((obs, oldVal, newVal) -> {
            btnAutoBid.setText(newVal ? "Bật" : "Tắt");
        });
    }

    /**
     * 🔥 HÀM TỰ ĐỘNG THÔNG MINH: Ưu tiên lấy ảnh từ UI, nếu chưa có sẽ tự động giải mã/nạp từ chuỗi gốc
     */
    public void initData(String name, String price, javafx.scene.image.Image directImage, String rawImageSource) {
        if (lblProductTitle != null) lblProductTitle.setText(name);
        if (lblCurrentPrice != null) lblCurrentPrice.setText(price);

        // Nếu là chuỗi Base64 thần thánh từ DB, giải mã luôn tại chỗ
        if (rawImageSource != null && rawImageSource.startsWith("base64:")) {
            byte[] bytes = java.util.Base64.getDecoder().decode(rawImageSource.substring(7));
            imgProduct.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes)));
        }
        // Nếu là tên file ảnh thật (như 1.jpg) thì lôi trong ổ C ra quét
        else if (rawImageSource != null && !rawImageSource.equals("null")) {
            java.io.File file = new java.io.File("C:/uet_uploads/" + rawImageSource);
            if (file.exists()) imgProduct.setImage(new javafx.scene.image.Image(file.toURI().toString()));
        }
        // Không có gì thì đập ảnh mặc định vào chống cháy
        else {
            imgProduct.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/com/auction/client/images/default.png")));
        }
    }

    public void loadProductDetail(Item item) {
        lblProductTitle.setText(item.getName());
        lblInfoName.setText(item.getName());
        lblInfoDescription.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả.");
        lblStartPrice.setText(String.format("%,.0f đ", item.getStartingPrice()));
        lblCurrentPrice.setText(String.format("%,.0f đ", item.getStartingPrice()));

        initData(item.getName(), String.format("%,.0f đ", item.getStartingPrice()), null, item.getImagePath());
    }

    public void loadProductDetail(Auction auction) {
        lblProductTitle.setText("Phiên đấu giá: " + auction.getItemId());
    }

    private void handleBid() {
        System.out.println("Đang đặt giá: " + txtBidAmount.getText());
    }

    @FXML
    private void handleBack() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().openHome();
        }
    }
}
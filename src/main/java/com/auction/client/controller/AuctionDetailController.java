package com.auction.client.controller;

import com.auction.shared.model.Item;
import com.auction.shared.model.Auction;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class AuctionDetailController {

    // Các thành phần UI khớp với FXML
    @FXML private Label lblProductTitle, lblTimeRemaining, lblInfoName, lblInfoDescription,
            lblStartPrice, lblSellerName, lblStartTime, lblEndTime,
            lblCurrentPrice, lblTopBidder;

    @FXML private ImageView imgProduct;
    @FXML private LineChart<Number, Number> chartPriceHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;
    @FXML private ToggleButton btnAutoBid;
    @FXML private VBox vboxBidHistoryContainer;

    /**
     * Hàm dùng để load dữ liệu sản phẩm vào trang chi tiết
     */
    public void loadProductDetail(Item item) {
        lblProductTitle.setText(item.getName());
        lblInfoName.setText(item.getName());
        lblInfoDescription.setText("Mô tả sản phẩm..."); // Hoặc item.getDescription()
        lblStartPrice.setText(String.format("%,.0f đ", item.getStartingPrice()));
        lblCurrentPrice.setText(String.format("%,.0f đ", item.getStartingPrice()));

        // Bạn có thể thêm logic load ảnh và lịch sử giá tại đây
    }

    public void loadProductDetail(Auction auction) {
        lblProductTitle.setText("Phiên đấu giá: " + auction.getItemId());
        // Map dữ liệu từ Auction object...
    }

    @FXML
    public void initialize() {
        // Thiết lập sự kiện cho nút đặt giá
        btnSubmitBid.setOnAction(event -> handleBid());

        // Thiết lập ToggleButton
        btnAutoBid.selectedProperty().addListener((obs, oldVal, newVal) -> {
            btnAutoBid.setText(newVal ? "Bật" : "Tắt");
        });
    }

    private void handleBid() {
        String amount = txtBidAmount.getText();
        System.out.println("Đang đặt giá: " + amount);
        // Gọi service để gửi lệnh đấu giá lên server tại đây
    }
}
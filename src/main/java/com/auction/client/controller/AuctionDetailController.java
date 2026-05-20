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
    @FXML public Label lblProductTitle, lblTimeRemaining, lblInfoName, lblInfoDescription,
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
    public void initData(String productName, String price) {
        // Đổ dữ liệu vào các Label trên giao diện chi tiết
        if (lblProductTitle != null) lblProductTitle.setText(productName);
        if (lblInfoName != null) lblInfoName.setText(productName);
        if (lblCurrentPrice != null) lblCurrentPrice.setText(price);
        if (lblStartPrice != null) lblStartPrice.setText(price);

        // Bạn có thể viết thêm logic xử lý đổ dữ liệu khác tại đây (ví dụ: mô tả, người bán...)
        System.out.println("Đã nạp dữ liệu vào trang chi tiết: " + productName + " - " + price);
    }
    @FXML
    private void handleBack() {
        if (MainLayoutController.getInstance() != null) {
            // Gọi hàm openHome() có sẵn trong MainLayoutController của bạn để nạp lại trang chủ
            MainLayoutController.getInstance().openHome();
        } else {
            System.err.println("Không thể quay lại vì MainLayoutController instance đang bị null!");
        }
    }
}
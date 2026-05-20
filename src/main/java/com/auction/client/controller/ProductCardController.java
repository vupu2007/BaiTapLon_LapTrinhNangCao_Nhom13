package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    // Hàm set dữ liệu cho Card
    public void setData(String name, String price, String time) {
        this.productName.setText(name);
        this.currentPrice.setText(price);
        this.timeRemaining.setText(time);

        // Gọi trực tiếp Singleton của MainLayout để mở trang chi tiết
        actionButton.setOnAction(e -> {
            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().openAuctionDetail(name, price);
            } else {
                System.err.println("MainLayoutController instance đang bị null!");
            }
        });
    }
}
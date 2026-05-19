package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import java.util.function.Consumer; // Sử dụng Functional Interface

public class ProductCardController {
    @FXML private ImageView productImage;
    @FXML private Label productName, productDesc, currentPrice, timeRemaining, statusBadge;
    @FXML private Button actionButton;

    // Biến Consumer để nhận lệnh từ MainController
    private Runnable onBidClicked;

    // Hàm set dữ liệu cho Card
    public void setData(String name, String price, String time, Runnable onBidClicked) {
        this.productName.setText(name);
        this.currentPrice.setText(price);
        this.timeRemaining.setText(time);
        this.onBidClicked = onBidClicked;

        // Khi nút được bấm, nó sẽ kích hoạt cái "Runnable" được truyền vào
        actionButton.setOnAction(e -> {
            if (this.onBidClicked != null) {
                this.onBidClicked.run();
            }
        });
    }
}
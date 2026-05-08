package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private TextField txtDeposit;
    @FXML private TextField txtWithdraw;

    @FXML
    private void handleDeposit() {
        String amount = txtDeposit.getText();
        System.out.println("Đang xử lý nạp: " + amount);
        // Thêm code cập nhật DB và thông báo ở đây
    }

    @FXML
    private void handleWithdraw() {
        String amount = txtWithdraw.getText();
        System.out.println("Đang xử lý rút: " + amount);
        // Thêm code cập nhật DB ở đây
    }
}
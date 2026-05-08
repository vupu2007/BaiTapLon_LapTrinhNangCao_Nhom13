package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Admin;
import com.auction.service.AuctionService;
import com.auction.util.CurrentUser;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class AuctionController {

    @FXML private TextField txtBidAmount;
    private AuctionService auctionService;

    public AuctionController() {
        this.auctionService = new AuctionService();
    }

    @FXML
    public void handlePlaceBid() {
        Account currentUser = CurrentUser.getUser();

        if (currentUser == null) {
            showAlert("Lỗi", "Bạn cần đăng nhập để đấu giá!");
            return;
        }

        if (currentUser instanceof Admin) {
            showAlert("Lỗi", "Admin không được phép đặt giá!");
            return;
        }

        try {
            double amount = Double.parseDouble(txtBidAmount.getText());
            String itemId = "1";

            // Truyền currentUser (Account) xuống Service
            boolean success = auctionService.placeBid(itemId, amount, currentUser);

            if (success) {
                showAlert("Thành công", "Bạn đã đặt giá thành công!");
                txtBidAmount.clear();
            } else {
                showAlert("Thất bại", "Đặt giá không thành công!");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void handleCloseAuction() {
        auctionService.closeAuction();
        showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
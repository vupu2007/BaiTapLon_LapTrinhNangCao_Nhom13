package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Admin;
import com.auction.observer.AuctionObserver;
import com.auction.service.AuctionService;
import com.auction.util.CurrentUser;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionController implements AuctionObserver {

    @FXML private TextField txtBidAmount;
    @FXML private Label lblCurrentPrice;  // Hiển thị giá hiện tại trên UI

    private final AuctionService auctionService = new AuctionService();
    private int currentAuctionId;

    public void setAuctionId(int auctionId) {
        this.currentAuctionId = auctionId;
        auctionService.addObserver(this);  // Đăng ký lắng nghe khi có bid mới
    }

    // Tự động chạy khi có bid mới — Observer nhận thông báo
    @Override
    public void onBidPlaced(int auctionId, double newPrice) {
        if (auctionId == currentAuctionId && lblCurrentPrice != null) {
            lblCurrentPrice.setText("Giá hiện tại: " + newPrice);
        }
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
            boolean success = auctionService.placeBid(currentAuctionId, amount, currentUser);

            if (success) {
                showAlert("Thành công", "Bạn đã đặt giá thành công!");
                txtBidAmount.clear();
            } else {
                showAlert("Thất bại", "Đặt giá không thành công. Kiểm tra lại số tiền hoặc số dư!");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void handleCloseAuction() {
        boolean success = auctionService.closeAuction(currentAuctionId);
        if (success) {
            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
        } else {
            showAlert("Lỗi", "Không thể đóng phiên đấu giá!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
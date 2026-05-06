package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.service.AuctionService;
import com.auction.util.CurrentAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class AuctionController {

    @FXML private TextField txtBidAmount; // Ô nhập giá tiền từ FXML

    private AuctionService auctionService;

    // Constructor mặc định để JavaFX không báo lỗi
    public AuctionController() {
        this.auctionService = new AuctionService();
    }

    /**
     * Xử lý khi người dùng nhấn nút "Đặt giá"
     */
    @FXML
    public void handlePlaceBid() {
        Account currentAccount = CurrentAccount.getAccount(); // Lấy người dùng hiện tại

        // 1. Kiểm tra xem người dùng đã đăng nhập chưa
        if (currentAccount == null) {
            showAlert("Lỗi", "Bạn cần đăng nhập để đấu giá!");
            return;
        }

        // 2. Chỉ có Bidder (người mua) mới được đặt giá
        if (!(currentAccount instanceof Bidder)) {
            showAlert("Lỗi", "Chỉ người mua mới có quyền đặt giá!");
            return;
        }

        try {
            double amount = Double.parseDouble(txtBidAmount.getText());

            // 3. Gọi Service để kiểm tra giá và lưu vào Database
            boolean success = auctionService.placeBid((Bidder) currentAccount, amount);

            if (success) {
                showAlert("Thành công", "Bạn đã đặt giá thành công!");
            } else {
                showAlert("Thất bại", "Giá đặt phải cao hơn giá hiện tại!");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void handleCloseAuction() {
        // Sau này có thể thêm kiểm tra: Chỉ Admin mới được đóng phiên
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
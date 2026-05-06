package com.auction.controller;

import com.auction.model.User; // Cần thêm dòng này
import com.auction.service.AuctionService; // Cần thêm dòng này
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
        User currentUser = CurrentUser.getUser();

        // 1. Kiểm tra đăng nhập
        if (currentUser == null) {
            showAlert("Lỗi", "Bạn cần đăng nhập để đấu giá!");
            return;
        }

        // 2. CHỈNH SỬA: Không chặn Seller nữa.
        // Logic chặn "tự đấu giá đồ mình bán" đã được đưa xuống Service xử lý.

        try {
            double amount = Double.parseDouble(txtBidAmount.getText());

            // Giả sử bạn lấy itemId từ sản phẩm đang được chọn trên giao diện
            // Ở đây mình tạm để là "1", bạn hãy thay bằng biến itemId thực tế nhé
            String itemId = "1";

            // 3. Gọi Service (truyền User chung vào)
            boolean success = auctionService.placeBid(itemId, amount, currentUser);

            if (success) {
                showAlert("Thành công", "Bạn đã đặt giá thành công!");
                txtBidAmount.clear(); // Xóa ô nhập sau khi thành công
            } else {
                showAlert("Thất bại", "Giá không hợp lệ hoặc bạn là chủ sở hữu món đồ này!");
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
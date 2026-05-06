package com.auction.controller;

import com.auction.model.User;
import com.auction.service.AuctionService;
import com.auction.util.CurrentUser;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuctionController {

    // --- KHAI BÁO CÁC BIẾN UI Ở ĐÂY ĐỂ DÙNG CHUNG TOÀN CLASS ---
    @FXML private VBox sidebar;
    @FXML private Button btnHome;
    @FXML private Button btnProducts;
    @FXML private Button btnCreate;
    @FXML private Button btnHistory;
    @FXML private TextField txtBidAmount;

    private AuctionService auctionService;
    private boolean isCollapsed = false; // Biến trạng thái đóng/mở

    public AuctionController() {
        this.auctionService = new AuctionService();
    }

    // --- LOGIC ĐÓNG/MỞ SIDEBAR ---
    @FXML
    private void toggleSidebar() {
        if (isCollapsed) {
            // MỞ RA: Đưa về kích thước ban đầu và hiện chữ
            sidebar.setMinWidth(250);
            sidebar.setPrefWidth(250);

            btnHome.setText("🏠  Trang chủ");
            btnProducts.setText("📦  Sản phẩm của tôi");
            btnCreate.setText("➕  Tạo phiên đấu giá");
            btnHistory.setText("🕘  Lịch sử");

            isCollapsed = false;
        } else {
            // ĐÓNG LẠI: Thu nhỏ và chỉ để lại Icon
            sidebar.setMinWidth(70);
            sidebar.setPrefWidth(70);

            btnHome.setText("🏠");
            btnProducts.setText("📦");
            btnCreate.setText("➕");
            btnHistory.setText("🕘");

            isCollapsed = true;
        }
    }

    // --- LOGIC ĐẤU GIÁ ---
    @FXML
    public void handlePlaceBid() {
        User currentUser = CurrentUser.getUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Bạn cần đăng nhập để đấu giá!");
            return;
        }

        try {
            double amount = Double.parseDouble(txtBidAmount.getText());
            String itemId = "1";
            boolean success = auctionService.placeBid(itemId, amount, currentUser);

            if (success) {
                showAlert("Thành công", "Bạn đã đặt giá thành công!");
                txtBidAmount.clear();
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
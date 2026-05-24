package com.auction.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import com.auction.shared.model.Observer;
import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;

public class AuctionController implements Observer {

    // 1. Các thành phần giao diện liên kết với file FXML
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLastBidder;
    @FXML private TextField txtBidAmount;

    // 2. Các đối tượng nghiệp vụ và thông tin phiên
    private Account currentAccount;
    private int currentAuctionId;

    /**
     * 3. Hàm khởi tạo: Nhận dữ liệu từ màn hình trước và ĐĂNG KÝ nhận thông báo
     */
    public void initData(int auctionId, Account account) {
        this.currentAuctionId = auctionId;
        this.currentAccount = account;
        // Đăng ký observer qua socket để nhận realtime update từ server
        try {
            Request req = new Request(MessageType.SUBSCRIBE_AUCTION, auctionId);
            ClientSocket.getInstance().sendRequest(req);
        } catch (Exception e) {
            System.err.println("Lỗi đăng ký observer: " + e.getMessage());
        }
    }

    /**
     * 4. Hàm Observer: Tự động chạy khi có người đặt giá thành công (từ luồng khác bắn sang)
     */
    @Override
    public void update(double newPrice, String username) {
        // Bắt buộc dùng Platform.runLater để vẽ lên giao diện JavaFX một cách an toàn
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) {
                lblCurrentPrice.setText("Giá hiện tại: " + newPrice);
            }
            if (lblLastBidder != null) {
                lblLastBidder.setText("Người đặt cao nhất: " + username);
            }
        });
    }

    /**
     * 5. Sự kiện nút Đặt Giá
     */
    @FXML
    public void handlePlaceBid() {
        Account currentUser = CurrentAccount.getAccount();

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
            Object[] data = {currentAuctionId, amount, currentUser.getId()};
            Request request = new Request(MessageType.PLACE_BID, data);
            Response response = ClientSocket.getInstance().sendRequest(request);

            if (response != null && response.isSuccess()) {
                txtBidAmount.clear();
            } else {
                String msg = (response != null) ? response.getMessage() : "Đặt giá không thành công!";
                showAlert("Thất bại", msg);
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            showAlert("Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    /**
     * 6. Sự kiện Đóng phiên đấu giá (Thường dành cho Admin hoặc Seller)
     */
    @FXML
    public void handleCloseAuction() {
        try {
            Request request = new Request(MessageType.CLOSE_AUCTION, currentAuctionId);
            Response response = ClientSocket.getInstance().sendRequest(request);
            if (response != null && response.isSuccess()) {
                showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
            } else {
                showAlert("Lỗi", "Không thể đóng phiên đấu giá!");
            }
        } catch (Exception e) {
            showAlert("Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    /**
     * 7. HÀM DỌN DẸP CHUNG: Luôn gọi hàm này trước khi rời đi để chống tràn RAM
     */
    public void cleanup() {
        try {
            Request req = new Request(MessageType.UNSUBSCRIBE_AUCTION, currentAuctionId);
            ClientSocket.getInstance().sendRequest(req);
            System.out.println("Đã hủy đăng ký nhận thông báo cho phòng " + currentAuctionId);
        } catch (Exception e) {
            System.err.println("Lỗi hủy observer: " + e.getMessage());
        }
    }

    /**
     * 8. Sự kiện nút Quay Lại / Thoát trên màn hình (Gắn vào onAction của nút Back)
     */
    @FXML
    public void handleBack(ActionEvent event) {
        // Xóa theo dõi trước
        cleanup();

        // Tắt màn hình hiện tại
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // --- Hàm tiện ích hỗ trợ hiển thị popup thông báo ---
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
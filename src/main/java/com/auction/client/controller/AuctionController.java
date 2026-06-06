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
import com.auction.client.util.CurrentAccount;
import com.auction.client.service.AuctionDetailService;

public class AuctionController implements Observer {

    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLastBidder;
    @FXML private TextField txtBidAmount;

    private final AuctionDetailService detailService = new AuctionDetailService();
    private Account currentAccount;
    private int currentAuctionId;

    public void initData(int auctionId, Account account) {
        this.currentAuctionId = auctionId;
        this.currentAccount = account;

        // Gọi hàm async
        detailService.subscribeAuctionAsync(auctionId, this, isSuccess -> {
            if (isSuccess) {
                System.out.println("-> Đã đăng ký lắng nghe Real-time cho phòng #" + auctionId);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể lắng nghe phòng này!");
            }
        });
    }

    @Override
    public void update(double newPrice, String username) {
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", newPrice));
            if (lblLastBidder != null) lblLastBidder.setText("Người đặt cao nhất: " + username);
        });
    }

    @FXML
    public void handlePlaceBid() {
        // 1. Lấy dữ liệu
        Account currentUser = CurrentAccount.getAccount();
        String bidText = txtBidAmount.getText().trim();

        // 2. Validate (Tách logic ra ngoài)
        if (!validateBidInput(currentUser, bidText)) {
            return;
        }

        // 3. Thực thi
        try {
            double amount = Double.parseDouble(bidText);

            // Disable nút để tránh người dùng click liên tục (Double submission)
            // btnPlaceBid.setDisable(true);

            detailService.sendBidRequestAsync(currentAuctionId, Integer.parseInt(currentUser.getId()), amount, response -> {
                // Đảm bảo cập nhật UI trên FX Thread
                Platform.runLater(() -> {
                    // btnPlaceBid.setDisable(false);
                    if (response != null && response.isSuccess()) {
                        txtBidAmount.clear();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Thất bại",
                                (response != null) ? response.getMessage() : "Kết nối đến máy chủ thất bại!");
                    }
                });
            });
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải là một con số hợp lệ!");
        }
    }

    private boolean validateBidInput(Account user, String bidText) {
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Bạn cần đăng nhập để đấu giá!");
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tài khoản Admin không được phép đấu giá!");
            return false;
        }
        if (bidText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập số tiền!");
            return false;
        }
        return true;
    }

    @FXML
    public void handleCloseAuction() {
        detailService.closeAuctionAsync(currentAuctionId, response -> {
            if (response != null && response.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Phiên đấu giá đã được kết thúc thành công!");
            } else {
                String msg = (response != null) ? response.getMessage() : "Không thể đóng phiên đấu giá!";
                showAlert(Alert.AlertType.ERROR, "Lỗi", msg);
            }
        });
    }

    @FXML
    public void handleBack(ActionEvent event) {
        detailService.unsubscribeAuctionAsync(currentAuctionId, this);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        // Vì callback trong service đã bọc Platform.runLater trước khi trả về,
        // nên ở đây gọi trực tiếp UI không lo bị crash luồng.
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
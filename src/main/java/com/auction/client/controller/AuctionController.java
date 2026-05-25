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

    // 1. Các thành phần giao diện liên kết với file FXML - Giữ nguyên
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLastBidder;
    @FXML private TextField txtBidAmount;

    // 2. Các đối tượng nghiệp vụ và thông tin phiên - Giữ nguyên
    private Account currentAccount;
    private int currentAuctionId;

    /**
     * 3. Hàm khởi tạo: Nhận dữ liệu từ màn hình trước và ĐĂNG KÝ nhận thông báo
     */
    public void initData(int auctionId, Account account) {
        this.currentAuctionId = auctionId;
        this.currentAccount = account;

        // Đăng ký nhận thông báo realtime chạy ngầm riêng, không làm nghẽn màn hình khi vừa mở phòng
        Thread initWorker = new Thread(() -> {
            try {
                Request req = new Request(MessageType.SUBSCRIBE_AUCTION, auctionId);
                ClientSocket.getInstance().sendRequest(req);
            } catch (Exception e) {
                System.err.println("❌ Lỗi đăng ký observer: " + e.getMessage());
            }
        });
        initWorker.setDaemon(true);
        initWorker.start();
    }

    /**
     * 4. Hàm Observer: Tự động chạy khi có người đặt giá thành công (Vẽ realtime từ server đẩy về)
     */
    @Override
    public void update(double newPrice, String username) {
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) {
                lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", newPrice));
            }
            if (lblLastBidder != null) {
                lblLastBidder.setText("Người đặt cao nhất: " + username);
            }
        });
    }

    /**
     * 5. Sự kiện nút Đặt Giá (Đã bọc luồng ngầm tránh đơ nút bấm)
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

            // Khởi tạo đúng Request 2 tham số của bạn
            Request request = new Request(MessageType.PLACE_BID, data);

            // Chạy luồng gửi tiền ngầm lên server
            Thread bidWorker = new Thread(() -> {
                try {
                    Response response = ClientSocket.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            txtBidAmount.clear();
                        } else {
                            String msg = (response != null) ? response.getMessage() : "Đặt giá không thành công!";
                            showAlert("Thất bại", msg);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi mạng", "Không thể kết nối đến máy chủ để đặt giá!"));
                }
            });
            bidWorker.setDaemon(true);
            bidWorker.start();

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    /**
     * 6. Sự kiện Đóng phiên đấu giá (Đã xử lý chạy ngầm an toàn)
     */
    @FXML
    public void handleCloseAuction() {
        Request request = new Request(MessageType.CLOSE_AUCTION, currentAuctionId);

        Thread closeWorker = new Thread(() -> {
            try {
                Response response = ClientSocket.getInstance().sendRequest(request);
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
                    } else {
                        showAlert("Lỗi", "Không thể đóng phiên đấu giá!");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi mạng", "Không thể kết nối đến máy chủ để đóng phòng!"));
            }
        });
        closeWorker.setDaemon(true);
        closeWorker.start();
    }

    /**
     * 7. HÀM DỌN DẸP CHUNG: Luôn gọi hàm này trước khi rời đi để chống tràn RAM
     */
    public void cleanup() {
        Thread cleanupWorker = new Thread(() -> {
            try {
                Request req = new Request(MessageType.UNSUBSCRIBE_AUCTION, currentAuctionId);
                ClientSocket.getInstance().sendRequest(req);
                System.out.println("Đã hủy đăng ký nhận thông báo cho phòng " + currentAuctionId);
            } catch (Exception e) {
                System.err.println("Lỗi hủy observer: " + e.getMessage());
            }
        });
        cleanupWorker.setDaemon(true);
        cleanupWorker.start();
    }

    /**
     * 8. Sự kiện nút Quay Lại / Thoát trên màn hình
     */
    @FXML
    public void handleBack(ActionEvent event) {
        // Xóa theo dõi realtime trước
        cleanup();

        // Tắt màn hình hiện tại
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // --- Hàm tiện ích hiển thị popup - Đảm bảo luôn được chạy trên JavaFX thread an toàn ---
    private void showAlert(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}
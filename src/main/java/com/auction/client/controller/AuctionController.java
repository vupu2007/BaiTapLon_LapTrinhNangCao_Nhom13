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

    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLastBidder;
    @FXML private TextField txtBidAmount;

    private Account currentAccount;
    private int currentAuctionId;

    /**
     * Đăng ký với cả Server (để lấy tin) và Local Client (để nhận tin)
     */
    public void initData(int auctionId, Account account) {
        this.currentAuctionId = auctionId;
        this.currentAccount = account;

        // Tách luồng đăng ký nhẹ để màn hình chuyển cảnh mượt mà, không bị khựng
        Thread initWorker = new Thread(() -> {
            try {
                // 1. Đăng ký với Server
                Request req = new Request(MessageType.SUBSCRIBE_AUCTION, auctionId);
                ClientSocket.getInstance().sendRequest(req);

                // 2. Đăng ký nội bộ với ClientSocket
                ClientSocket.getInstance().addAuctionObserver(auctionId, this);
                System.out.println("-> Đã đăng ký lắng nghe Real-time cho phòng #" + auctionId);
            } catch (Exception e) {
                System.err.println("❌ Lỗi đăng ký observer: " + e.getMessage());
            }
        });
        initWorker.setDaemon(true);
        initWorker.start();
    }

    @Override
    public void update(double newPrice, String username) {
        // Luôn cập nhật giao diện Real-time an toàn từ luồng JavaFX
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) {
                lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", newPrice));
            }
            if (lblLastBidder != null) {
                lblLastBidder.setText("Người đặt cao nhất: " + username);
            }
        });
    }

    @FXML
    public void handlePlaceBid() {
        Account currentUser = CurrentAccount.getAccount();

        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Bạn cần đăng nhập để đấu giá!");
            return;
        }
        if (currentUser instanceof Admin || "ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tài khoản Admin không được phép tham gia đặt giá!");
            return;
        }

        String bidText = txtBidAmount.getText().trim();
        if (bidText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập số tiền muốn đấu giá!");
            return;
        }

        try {
            double amount = Double.parseDouble(bidText);
            Object[] data = {currentAuctionId, amount, currentUser.getId()};
            Request request = new Request(MessageType.PLACE_BID, data);

            // 🚀 TỐI ƯU: Đẩy lệnh gửi tiền xuống luồng riêng để nút bấm không bị "đơ"
            Thread bidWorker = new Thread(() -> {
                try {
                    Response response = ClientSocket.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            txtBidAmount.clear();
                            // Không cần gọi update() thủ công, Server sẽ tự broadcast giá mới về cho mọi người
                        } else {
                            String msg = (response != null) ? response.getMessage() : "Đặt giá không thành công!";
                            showAlert(Alert.AlertType.ERROR, "Thất bại", msg);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu đặt giá đến máy chủ!");
                    });
                }
            }, "BidNetworkWorker");
            bidWorker.setDaemon(true);
            bidWorker.start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số tiền hợp lệ (Ví dụ: 500000)!");
        }
    }

    @FXML
    public void handleCloseAuction() {
        // 🚀 TỐI ƯU: Chạy ngầm tác vụ đóng phiên đấu giá
        Thread closeWorker = new Thread(() -> {
            try {
                Request request = new Request(MessageType.CLOSE_AUCTION, currentAuctionId);
                Response response = ClientSocket.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Phiên đấu giá đã được kết thúc thành công!");
                    } else {
                        String msg = (response != null) ? response.getMessage() : "Không thể đóng phiên đấu giá!";
                        showAlert(Alert.AlertType.ERROR, "Lỗi", msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ để đóng phiên!");
                });
            }
        }, "CloseAuctionWorker");
        closeWorker.setDaemon(true);
        closeWorker.start();
    }

    /**
     * Hủy đăng ký cả ở Server lẫn Local Client khi rời phòng
     */
    public void cleanup() {
        Thread cleanupWorker = new Thread(() -> {
            try {
                // 1. Báo Server ngừng gửi tin
                Request req = new Request(MessageType.UNSUBSCRIBE_AUCTION, currentAuctionId);
                ClientSocket.getInstance().sendRequest(req);

                // 2. Gỡ bỏ Controller khỏi danh sách chờ nội bộ
                ClientSocket.getInstance().removeAuctionObserver(currentAuctionId, this);

                System.out.println("-> Đã hủy dọn dẹp Observer thành công phòng #" + currentAuctionId);
            } catch (Exception e) {
                System.err.println("❌ Lỗi hủy observer khi cleanup: " + e.getMessage());
            }
        });
        cleanupWorker.setDaemon(true);
        cleanupWorker.start();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        cleanup(); // Thực hiện gỡ dọn dẹp kết nối ngầm trước khi đóng cửa sổ
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // Đảm bảo hiển thị popup an toàn từ bất kỳ luồng nào
    private void showAlert(Alert.AlertType type, String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}
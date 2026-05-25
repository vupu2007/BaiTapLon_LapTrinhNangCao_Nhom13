package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActiveAuctionsController {

    @FXML private VBox emptyStateBox;
    @FXML private FlowPane cardsContainer;

    // Bộ định dạng ngày giờ để hiển thị cho đẹp: dd/MM/yyyy HH:mm
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        loadAuctions();
    }

    private void loadAuctions() {
        // Xóa sạch các card cũ trước khi load để tránh trùng lặp dữ liệu
        if (cardsContainer != null) {
            cardsContainer.getChildren().clear();
        }

        // Lấy thông tin tài khoản đang đăng nhập hệ thống hiện tại
        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc == null) {
            showEmptyState(true);
            return;
        }

        try {
            // Đã sửa: Ép kiểu ID tài khoản từ String sang int để gọi khớp với AuctionDAO
            int bidderIdInt = Integer.parseInt(currentAcc.getId());

            // Gửi request lấy danh sách phiên đấu giá theo bidder qua Socket
            Request request = new Request(MessageType.GET_AUCTIONS_BY_BIDDER, bidderIdInt);
            Response response = ClientSocket.getInstance().sendRequest(request);

            if (response == null || !response.isSuccess()) {
                showEmptyState(true);
                return;
            }

            // Chỉ lấy những phiên mà tài khoản này ĐÃ ĐẶT GIÁ THÀNH CÔNG
            List<Auction> auctions = (List<Auction>) response.getData();

            if (auctions == null || auctions.isEmpty()) {
                showEmptyState(true);
                return;
            }

            showEmptyState(false);

            for (Auction auction : auctions) {
                try {
                    // Lấy thông tin Item qua Socket
                    Request itemRequest = new Request(MessageType.GET_ITEM_BY_ID, auction.getItemId());
                    Response itemResponse = ClientSocket.getInstance().sendRequest(itemRequest);
                    Item item = (itemResponse != null && itemResponse.isSuccess())
                            ? (Item) itemResponse.getData() : null;

                    // Load ProductCard.fxml
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                    Node card = loader.load();
                    ProductCardController controller = loader.getController();

                    String name        = (item != null) ? item.getName()        : "Sản phẩm #" + auction.getItemId();
                    String image       = (item != null) ? item.getImagePath()   : null;
                    String description = (item != null) ? item.getDescription() : "Không có mô tả.";
                    String sellerName  = (item != null) ? "Người bán #" + item.getOwnerId() : "Ẩn danh";

                    // Định dạng ngày giờ bắt đầu và kết thúc của phiên đấu giá thành chuỗi chữ
                    String startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                    String endTimeStr   = (auction.getEndTime()   != null) ? auction.getEndTime().format(dateTimeFormatter)   : "--/--/---- --:--";

                    // Tính thời gian còn lại hiển thị ở ngoài card
                    long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                    String time  = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";

                    // Định dạng giá tiền
                    String price = String.format("%,.0f VNĐ", auction.getCurrentPrice());

                    // Nạp ĐỦ 8 THAM SỐ vào hàm setData của Card
                    controller.setData(name, price, time, image, description, sellerName, startTimeStr, endTimeStr);
                    cardsContainer.getChildren().add(card);

                } catch (Exception e) {
                    System.err.println("❌ Lỗi load card auction_id=" + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ ID Tài khoản không hợp lệ: " + currentAcc.getId());
            showEmptyState(true);
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối server: " + e.getMessage());
            showEmptyState(true);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(isEmpty);
            emptyStateBox.setManaged(isEmpty);
        }
        if (cardsContainer != null) {
            cardsContainer.setVisible(!isEmpty);
            cardsContainer.setManaged(!isEmpty);
        }
    }

    @FXML
    private void openHome() {
        if (MainLayoutController.getInstance() != null) {
            javafx.application.Platform.runLater(() -> {
                MainLayoutController.getInstance().openHome();
            });
        }
    }
}

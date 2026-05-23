package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
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

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO itemDAO = new ItemDAO();

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

            // Chỉ lấy những phiên mà tài khoản này ĐÃ ĐẶT GIÁ THÀNH CÔNG
            List<Auction> auctions = auctionDAO.getAuctionsByBidder(bidderIdInt);

            if (auctions == null || auctions.isEmpty()) {
                showEmptyState(true);
                return;
            }

            showEmptyState(false);

            for (Auction auction : auctions) {
                try {
                    // Load ProductCard.fxml
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                    Node card = loader.load();
                    ProductCardController controller = loader.getController();

                    // Đã sửa: Truyền thẳng auction.getItemId() (vì nó là String sẵn rồi), fix triệt để lỗi Incompatible types
                    Item item = itemDAO.getItemById(auction.getItemId());
                    String name  = (item != null) ? item.getName() : "Sản phẩm #" + auction.getItemId();
                    String image = (item != null) ? item.getImagePath() : null;

                    // Trích xuất dữ liệu nâng cao đầy đủ tham số
                    String description = (item != null) ? item.getDescription() : "Không có mô tả.";
                    String sellerName = (item != null) ? "Người bán #" + item.getOwnerId() : "Ẩn danh";

                    // Định dạng ngày giờ bắt đầu và kết thúc của phiên đấu giá thành chuỗi chữ
                    String startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                    String endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";

                    // Tính thời gian còn lại hiển thị ở ngoài card
                    long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                    String time  = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";

                    // Định dạng giá tiền
                    String price = String.format("%,.0f VNĐ", auction.getCurrentPrice());

                    // Nạp chuẩn đét ĐỦ 8 THAM SỐ vào hàm setData của Card
                    controller.setData(name, price, time, image, description, sellerName, startTimeStr, endTimeStr);

                    cardsContainer.getChildren().add(card);

                } catch (Exception e) {
                    System.err.println("❌ Lỗi load card auction_id=" + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ ID Tài khoản không hợp lệ (Không phải số nguyên): " + currentAcc.getId());
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
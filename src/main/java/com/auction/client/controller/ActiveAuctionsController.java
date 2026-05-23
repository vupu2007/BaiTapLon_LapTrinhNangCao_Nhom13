package com.auction.client.controller;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
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
        List<Auction> auctions = auctionDAO.getAuctionsByStatus(AuctionStatus.RUNNING);

        if (auctions.isEmpty()) {
            showEmptyState(true);
            return;
        }

        showEmptyState(false);

        for (Auction auction : auctions) {
            try {
                // Load ProductCard.fxml
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/ProductCard.fxml")
                );
                Node card = loader.load();
                ProductCardController controller = loader.getController();

                // Lấy thông tin item từ DB
                Item item = itemDAO.getItemById(auction.getItemId());
                String name  = (item != null) ? item.getName() : "Sản phẩm #" + auction.getItemId();
                String image = (item != null) ? item.getImagePath() : null;

                // 🔥 TRÍCH XUẤT THÊM DỮ LIỆU ĐỂ ĐỦ 8 THAM SỐ
                String description = (item != null) ? item.getDescription() : "Không có mô tả.";

                // Giả sử item có lưu thông tin người bán (sellerId hoặc sellerName).
                // Nếu chưa có bảng User/Seller cụ thể, má cứ tạm thời để tên người bán là "Chủ phòng #" + item.getOwnerId() hoặc tên thật nếu có trường name.
                String sellerName = (item != null) ? "Người bán #" + item.getOwnerId() : "Ẩn danh";

                // Định dạng ngày giờ bắt đầu và kết thúc của phiên đấu giá thành chuỗi chữ
                String startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                String endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";

                // Tính thời gian còn lại hiển thị ở ngoài card
                long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                String time  = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";

                // Định dạng giá
                String price = String.format("%,.0f VNĐ", auction.getCurrentPrice());

                // 🔥 ĐÃ SỬA: Truyền chuẩn đét ĐỦ 8 THAM SỐ vào hàm setData mới nâng cấp
                controller.setData(name, price, time, image, description, sellerName, startTimeStr, endTimeStr);

                cardsContainer.getChildren().add(card);

            } catch (Exception e) {
                System.err.println("❌ Lỗi load card auction_id=" + auction.getId() + ": " + e.getMessage());
            }
        }
    }

    private void showEmptyState(boolean isEmpty) {
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);
        cardsContainer.setVisible(!isEmpty);
        cardsContainer.setManaged(!isEmpty);
    }

    @FXML
    private void openHome() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().openHome();
        }
    }
}
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
import javafx.application.Platform;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActiveAuctionsController {

    @FXML private VBox emptyStateBox;
    @FXML private FlowPane cardsContainer;

    // Bộ định dạng ngày giờ của bạn - Giữ nguyên
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        loadAuctions();
    }

    private void loadAuctions() {
        if (cardsContainer != null) {
            cardsContainer.getChildren().clear();
        }

        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc == null) {
            showEmptyState(true);
            return;
        }

        // 🚀 Tách một Thread chạy ngầm để kéo dữ liệu mạng, không làm đơ giao diện UI
        Thread networkWorker = new Thread(() -> {
            try {
                int bidderIdInt = Integer.parseInt(currentAcc.getId());

                // Gửi request lấy danh sách phiên đấu giá
                Request request = new Request(MessageType.GET_AUCTIONS_BY_BIDDER, bidderIdInt);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response == null || !response.isSuccess()) {
                    Platform.runLater(() -> showEmptyState(true));
                    return;
                }

                List<Auction> auctions = (List<Auction>) response.getData();

                if (auctions == null || auctions.isEmpty()) {
                    Platform.runLater(() -> showEmptyState(true));
                    return;
                }

                // Cập nhật trạng thái ẩn trạng thái trống trên luồng UI
                Platform.runLater(() -> showEmptyState(false));

                // Duyệt danh sách phiên đấu giá ngầm dưới RAM
                for (Auction auction : auctions) {
                    try {
                        // Lấy thông tin Item qua Socket ngầm
                        Request itemRequest = new Request(MessageType.GET_ITEM_BY_ID, auction.getItemId());
                        Response itemResponse = ClientSocket.getInstance().sendRequest(itemRequest);
                        Item item = (itemResponse != null && itemResponse.isSuccess())
                                ? (Item) itemResponse.getData() : null;

                        // Xử lý chuỗi và tính toán thời gian của bạn - Giữ nguyên logic gốc
                        String name        = (item != null) ? item.getName()        : "Sản phẩm #" + auction.getItemId();
                        String image       = (item != null) ? item.getImagePath()   : null;
                        String description = (item != null) ? item.getDescription() : "Không có mô tả.";
                        String sellerName  = (item != null) ? "Người bán #" + item.getOwnerId() : "Ẩn danh";

                        String startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                        String endTimeStr   = (auction.getEndTime()   != null) ? auction.getEndTime().format(dateTimeFormatter)   : "--/--/---- --:--";

                        long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                        String time  = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
                        String price = String.format("%,.0f VNĐ", auction.getCurrentPrice());

                        // 🚀 Đẩy việc nạp giao diện và hiển thị Card quay lại luồng JavaFX an toàn
                        Platform.runLater(() -> {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                                Node card = loader.load();
                                ProductCardController controller = loader.getController();

                                // Nạp đủ 8 tham số chuẩn của bạn
                                controller.setData(name, price, time, image, description, sellerName, startTimeStr, endTimeStr);
                                cardsContainer.getChildren().add(card);
                            } catch (Exception e) {
                                System.err.println("❌ Lỗi dựng UI hiển thị Card: " + e.getMessage());
                            }
                        });

                    } catch (Exception e) {
                        System.err.println("❌ Lỗi load dữ liệu mạng auction_id=" + auction.getId() + ": " + e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("❌ ID Tài khoản không hợp lệ: " + currentAcc.getId());
                Platform.runLater(() -> showEmptyState(true));
            } catch (Exception e) {
                System.err.println("❌ Lỗi kết nối mạng: " + e.getMessage());
                Platform.runLater(() -> showEmptyState(true));
            }
        }, "ActiveAuctionsLoaderThread");

        networkWorker.setDaemon(true);
        networkWorker.start();
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
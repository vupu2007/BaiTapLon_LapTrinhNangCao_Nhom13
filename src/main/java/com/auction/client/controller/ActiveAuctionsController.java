package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActiveAuctionsController {

    // Sử dụng Logger chuẩn thay cho System.err.println / printStackTrace
    private static final Logger LOGGER = Logger.getLogger(ActiveAuctionsController.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox emptyStateBox;
    @FXML private FlowPane cardsContainer;

    // LỚP TRUNG GIAN : Dùng để bọc dữ liệu thô từ luồng ngầm gửi về cho luồng UI dựng Card
    private static class AuctionCardDto {
        String name;
        String price;
        String time;
        String image;
        String description;
        String sellerName;
        String startTimeStr;
        String endTimeStr;
        Auction auction;
    }

    @FXML
    public void initialize() {
        loadAuctions();
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

    /**
     * Điều hướng quay lại trang chủ bằng kỹ thuật Scene Graph Lookup
     */
    @FXML
    private void openHome() {
        if (cardsContainer == null || cardsContainer.getScene() == null) return;

        Platform.runLater(() -> {
            try {
                String homeFxmlPath = "/view/MainView.fxml";
                java.net.URL fxmlLocation = getClass().getResource(homeFxmlPath);
                if (fxmlLocation == null) {
                    LOGGER.warning("❌ LỖI: Không tìm thấy tệp FXML trang chủ tại: " + homeFxmlPath);
                    return;
                }

                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Node homeView = loader.load();

                // Dò tìm động phân vùng chứa trung tâm #contentArea từ cây phân cấp giao diện hiện tại
                Parent root = cardsContainer.getScene().getRoot();
                Node layoutCenter = root.lookup("#contentArea");

                if (layoutCenter instanceof StackPane contentArea) {
                    // Chèn màn hình tổng quan trang chủ quay lại trung tâm màn hình Layout chính
                    contentArea.getChildren().setAll(homeView);
                    LOGGER.info("🎯 [Navigation] Đã chuyển đổi màn hình sang Trang chủ từ ActiveAuctions.");
                } else {
                    LOGGER.warning("❌ Không định vị được vùng hiển thị #contentArea trên giao diện.");
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Lỗi nghiêm trọng khi quay lại giao diện trang chủ", e);
            }
        });
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

        Task<List<AuctionCardDto>> loadTask = new Task<>() {
            @Override
            protected List<AuctionCardDto> call() throws Exception {
                List<AuctionCardDto> dataList = new ArrayList<>();
                int bidderIdInt = Integer.parseInt(currentAcc.getId());

                Request request = new Request(MessageType.GET_AUCTIONS_BY_BIDDER, bidderIdInt);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response == null || !response.isSuccess()) return dataList;
                if (!(response.getData() instanceof List<?> rawList)) return dataList;

                List<Auction> auctions = new ArrayList<>();
                for (Object obj : rawList) {
                    if (obj instanceof Auction a) auctions.add(a);
                }
                if (auctions.isEmpty()) return dataList;

                // FIX WARNING 3: Thay .get(0) bằng .getFirst() (Java 21+)
                LOGGER.info(() -> "DEBUG size=" + auctions.size() + " name0=" + auctions.getFirst().getProductName());

                // Gỡ bỏ try-catch con ở đây để tránh WARNING 2 (Hãy để Task tự xử lý nếu crash)
                for (Auction auction : auctions) {
                    AuctionCardDto dto = new AuctionCardDto();
                    dto.name = auction.getProductName() != null ? auction.getProductName() : "Sản phẩm #" + auction.getItemId();
                    dto.image = auction.getImagePath();
                    dto.description = auction.getDescription() != null ? auction.getDescription() : "Không có mô tả.";
                    dto.sellerName = auction.getSellerName() != null ? auction.getSellerName() : "Người bán #" + auction.getSellerId();
                    dto.startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(DATE_TIME_FORMATTER) : "--/--/---- --:--";
                    dto.endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(DATE_TIME_FORMATTER) : "--/--/---- --:--";

                    long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                    dto.time = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
                    dto.price = String.format("%,.0f VNĐ", auction.getCurrentPrice());
                    dto.auction = auction;
                    dataList.add(dto);
                }
                return dataList;
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<AuctionCardDto> results = loadTask.getValue();
            if (results == null || results.isEmpty()) {
                showEmptyState(true);
                return;
            }
            showEmptyState(false);

            for (AuctionCardDto dto : results) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                    Node card = loader.load();
                    ProductCardController controller = loader.getController();

                    if (controller != null) {
                        String statusText = "Đang diễn ra";
                        if (dto.auction.getEndTime() != null && LocalDateTime.now().isAfter(dto.auction.getEndTime())) {
                            statusText = "Đã kết thúc";
                        }
                        controller.setData(dto.name, dto.price, statusText, dto.image, dto.description,
                                dto.sellerName, dto.startTimeStr, dto.endTimeStr);
                    } else {
                        LOGGER.warning("DEBUG controller null");
                    }

                    cardsContainer.getChildren().add(card);

                    card.setOnMouseClicked(e -> {
                        if (dto.auction == null) return;
                        try {
                            FXMLLoader detailLoader = new FXMLLoader(getClass().getResource("/view/AuctionDetail.fxml"));
                            Parent detailView = detailLoader.load();
                            AuctionDetailController detailController = detailLoader.getController();

                            dto.auction.setProductName(dto.name);
                            dto.auction.setSellerName(dto.sellerName);
                            dto.auction.setDescription(dto.description);

                            // FIX WARNING 4 & 5: Điền log vào block catch trống để dễ debug
                            if (dto.startTimeStr != null) {
                                try {
                                    dto.auction.setStartTime(LocalDateTime.parse(dto.startTimeStr, DATE_TIME_FORMATTER));
                                } catch (Exception ex) {
                                    LOGGER.log(Level.FINE, "Không thể parse StartTime", ex);
                                }
                            }
                            if (dto.endTimeStr != null) {
                                try {
                                    dto.auction.setEndTime(LocalDateTime.parse(dto.endTimeStr, DATE_TIME_FORMATTER));
                                } catch (Exception ex) {
                                    LOGGER.log(Level.FINE, "Không thể parse EndTime", ex);
                                }
                            }

                            if (detailController != null) {
                                detailController.loadProductDetail(dto.auction);
                            }

                            Parent root = card.getScene().getRoot();
                            Node center = root.lookup("#contentArea");
                            if (center instanceof StackPane contentArea) {
                                contentArea.getChildren().setAll(detailView);
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "❌ Lỗi chuyển màn hình chi tiết", ex);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "❌ Lỗi dựng card", e);
                }
            }
        });

        // Hỗ trợ bắt toàn bộ lỗi phát sinh từ hàm call() luồng ngầm
        loadTask.setOnFailed(event -> {
            LOGGER.log(Level.SEVERE, "❌ Lỗi Task chạy ngầm thất bại", loadTask.getException());
            showEmptyState(true);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }
}
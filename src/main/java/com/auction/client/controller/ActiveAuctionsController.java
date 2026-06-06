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

public class ActiveAuctionsController {

    @FXML private VBox emptyStateBox;
    @FXML private FlowPane cardsContainer;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    // LỚP TRUNG GIAN (DTO): Dùng để bọc dữ liệu thô từ luồng ngầm gửi về cho luồng UI dựng Card
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
                    System.err.println("❌ LỖI: Không tìm thấy tệp FXML trang chủ tại: " + homeFxmlPath);
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
                    System.out.println("🎯 [Navigation] Đã chuyển đổi màn hình sang Trang chủ từ ActiveAuctions thông qua Scene Graph Lookup.");
                } else {
                    System.err.println("❌ Không định vị được vùng hiển thị #contentArea trên giao diện.");
                }

            } catch (Exception e) {
                System.err.println("❌ Lỗi nghiêm trọng khi quay lại giao diện trang chủ: " + e.getMessage());
                e.printStackTrace();
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
                System.out.println("DEBUG size=" + auctions.size() + " name0=" + auctions.get(0).getProductName());

                for (Auction auction : auctions) {
                    try {
                        AuctionCardDto dto = new AuctionCardDto();
                        dto.name = auction.getProductName() != null ? auction.getProductName() : "Sản phẩm #" + auction.getItemId();
                        dto.image = auction.getImagePath();
                        dto.description = auction.getDescription() != null ? auction.getDescription() : "Không có mô tả.";
                        dto.sellerName = auction.getSellerName() != null ? auction.getSellerName() : "Người bán #" + auction.getSellerId();
                        dto.startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                        dto.endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";
                        long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                        dto.time = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
                        dto.price = String.format("%,.0f VNĐ", auction.getCurrentPrice());
                        dto.auction = auction;
                        dataList.add(dto);
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi: " + e.getMessage());
                    }
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
                        //controller.setOriginProductData(dto.auction);
                    }
                    else {
                        System.out.println("DEBUG controller null");
                    }

                    cardsContainer.getChildren().add(card);
                    card.setOnMouseClicked(e -> {
                        if (dto.auction == null) return;
                        try {
                            FXMLLoader detailLoader = new FXMLLoader(getClass().getResource("/view/AuctionDetail.fxml"));
                            Parent detailView = detailLoader.load();
                            AuctionDetailController detailController = detailLoader.getController();

                            // 🎯 BƠM ĐẦY ĐỦ NGUYÊN LIỆU XỊN VÀO ĐÂY
                            dto.auction.setProductName(dto.name);       // Ép tên SP (Tránh lỗi Sản phẩm #0)
                            dto.auction.setSellerName(dto.sellerName);   // Ép tên người bán
                            dto.auction.setDescription(dto.description); // Ép mô tả

                            // Parse thời gian từ Chuỗi hiển thị ngoài Card thành Object LocalDateTime chuẩn
                            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                            if (dto.startTimeStr != null) {
                                try { dto.auction.setStartTime(java.time.LocalDateTime.parse(dto.startTimeStr, dtf)); } catch (Exception ex) {}
                            }
                            if (dto.endTimeStr != null) {
                                try { dto.auction.setEndTime(java.time.LocalDateTime.parse(dto.endTimeStr, dtf)); } catch (Exception ex) {}
                            }

                            if (detailController != null) {
                                detailController.loadProductDetail(dto.auction);
                            }

                            // Chuyển màn hình
                            Parent root = card.getScene().getRoot();
                            Node center = root.lookup("#contentArea");
                            if (center instanceof StackPane contentArea) {
                                contentArea.getChildren().setAll(detailView);
                            }
                        } catch (Exception ex) {
                            System.err.println("❌ Lỗi chuyển màn hình chi tiết: " + ex.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("❌ Lỗi dựng card: " + e.getMessage());
                }
            }
        });

        loadTask.setOnFailed(event -> {
            System.err.println("❌ Lỗi: " + loadTask.getException().getMessage());
            showEmptyState(true);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }
}
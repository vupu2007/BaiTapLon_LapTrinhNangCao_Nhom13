package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
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

                for (Auction auction : auctions) {
                    try {
                        Request itemRequest = new Request(MessageType.GET_ITEM_BY_ID, auction.getItemId());
                        Response itemResponse = ClientSocket.getInstance().sendRequest(itemRequest);
                        Item item = (itemResponse != null && itemResponse.isSuccess())
                                ? (Item) itemResponse.getData() : null;

                        AuctionCardDto dto = new AuctionCardDto();
                        dto.name = (item != null) ? item.getName() : "Sản phẩm #" + auction.getItemId();
                        dto.image = (item != null) ? item.getImagePath() : null;
                        dto.description = (item != null) ? item.getDescription() : "Không có mô tả.";
                        dto.sellerName = (item != null) ? "Người bán #" + item.getOwnerId() : "Ẩn danh";

                        dto.startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                        dto.endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";

                        long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                        dto.time = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
                        dto.price = String.format("%,.0f VNĐ", auction.getCurrentPrice());
                        dto.auction = auction;

                        dataList.add(dto);
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi lấy dữ liệu mạng của auction_id=" + auction.getId() + ": " + e.getMessage());
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
                    final Auction currentAuction = dto.auction;
                    if (currentAuction == null) continue;

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                    Node card = loader.load();
                    ProductCardController controller = loader.getController();

                    if (controller != null) {
                        controller.setData(dto.name, dto.price, dto.time, dto.image, dto.description,
                                dto.sellerName, dto.startTimeStr, dto.endTimeStr);
                    }

                    cardsContainer.getChildren().add(card);

                    // 🎯 ĐÃ SỬA: Cơ chế khóa chống spam click chuột lan truyền phần tử con
                    card.setOnMouseClicked(e -> {
                        // Kiểm tra dữ liệu hợp lệ ngay lập tức
                        if (currentAuction == null || currentAuction.getId() <= 0) {
                            e.consume();
                            return;
                        }

                        // Khóa tương tác của Card tạm thời để tránh người dùng nhấn liên tục khi mạng trễ
                        card.setDisable(true);

                        Task<Auction> detailTask = new Task<>() {
                            @Override
                            protected Auction call() throws Exception {
                                Response res = ClientSocket.getInstance().sendRequest(
                                        new Request(MessageType.GET_AUCTION_BY_ID, currentAuction.getId()));
                                if (res != null && res.isSuccess()) {
                                    return (Auction) res.getData();
                                }
                                return null;
                            }
                        };

                        detailTask.setOnSucceeded(de -> {
                            // Mở khóa lại Card sau khi tiến trình ngầm hoàn tất
                            card.setDisable(false);

                            Auction fullAuction = detailTask.getValue();
                            if (fullAuction == null || fullAuction.getId() <= 0) {
                                System.err.println("⚠️ CẢNH BÁO: Hủy chuyển trang do Server trả về Object lỗi.");
                                return;
                            }

                            Platform.runLater(() -> {
                                try {
                                    Parent root = card.getScene().getRoot();
                                    Node contentAreaNode = root.lookup("#contentArea");

                                    if (contentAreaNode != null && contentAreaNode.getScene() != null) {
                                        String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                                                ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";

                                        FXMLLoader detailLoader = new FXMLLoader(getClass().getResource(path));
                                        Parent detailView = detailLoader.load();

                                        AuctionDetailController detailController = detailLoader.getController();
                                        if (detailController != null) {
                                            detailController.loadProductDetail(fullAuction);
                                        }

                                        if (contentAreaNode instanceof StackPane contentArea) {
                                            contentArea.getChildren().setAll(detailView);
                                        }
                                    }
                                } catch (Exception ex) {
                                    System.err.println("❌ Lỗi chuyển giao diện: " + ex.getMessage());
                                }
                            });
                        });

                        // Giải phóng nút bấm nếu luồng mạng xảy ra lỗi kết nối
                        detailTask.setOnFailed(df -> {
                            card.setDisable(false);
                            System.err.println("❌ Lỗi luồng mạng tải chi tiết phiên ID: " + currentAuction.getId());
                        });

                        Thread t = new Thread(detailTask);
                        t.setDaemon(true);
                        t.start();
                    });
                } catch (Exception e) {
                    System.err.println("❌ Lỗi dựng giao diện Card từ FXML: " + e.getMessage());
                }
            }
        });

        loadTask.setOnFailed(event -> {
            System.err.println("❌ Lỗi luồng ngầm ActiveAuctions: " + loadTask.getException().getMessage());
            showEmptyState(true);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
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
        if (cardsContainer == null || cardsContainer.getScene() == null) return;

        Platform.runLater(() -> {
            try {
                String homeFxmlPath = "/view/MainView.fxml";
                java.net.URL fxmlLocation = getClass().getResource(homeFxmlPath);
                if (fxmlLocation == null) return;

                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Node homeView = loader.load();

                Parent root = cardsContainer.getScene().getRoot();
                Node layoutCenter = root.lookup("#contentArea");

                if (layoutCenter instanceof StackPane contentArea) {
                    contentArea.getChildren().setAll(homeView);
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi quay lại giao diện trang chủ: " + e.getMessage());
            }
        });
    }
}
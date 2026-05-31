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


    // 🌟 LỚP TRUNG GIAN (DTO): Dùng để bọc dữ liệu thô từ luồng ngầm gửi về cho luồng UI dựng Card
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

        // 🌟 KHỞI TẠO TASK: Luồng này chỉ lấy dữ liệu mạng thuần túy, tuyệt đối không tạo Node UI ở đây
        Task<List<AuctionCardDto>> loadTask = new Task<>() {
            @Override
            protected List<AuctionCardDto> call() throws Exception {
                List<AuctionCardDto> dataList = new ArrayList<>();
                int bidderIdInt = Integer.parseInt(currentAcc.getId());

                // 1. Lấy danh sách các phiên đấu giá từ Server
                Request request = new Request(MessageType.GET_AUCTIONS_BY_BIDDER, bidderIdInt);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response == null || !response.isSuccess()) return dataList;

                // Kiểm tra kiểu dữ liệu an toàn trước khi ép kiểu danh sách phiên đấu giá
                if (!(response.getData() instanceof List<?> rawList)) return dataList;

                List<Auction> auctions = new ArrayList<>();
                for (Object obj : rawList) {
                    if (obj instanceof Auction a) auctions.add(a);
                }

                if (auctions.isEmpty()) return dataList;

                // 2. Vòng lặp lấy thông tin Item tương ứng (Chạy ngầm tuần tự)
                for (Auction auction : auctions) {
                    try {
                        Request itemRequest = new Request(MessageType.GET_ITEM_BY_ID, auction.getItemId());
                        Response itemResponse = ClientSocket.getInstance().sendRequest(itemRequest);
                        Item item = (itemResponse != null && itemResponse.isSuccess())
                                ? (Item) itemResponse.getData() : null;

                        // Gom tất cả thông tin dạng String/Dữ liệu thô vào đối tượng DTO
                        AuctionCardDto dto = new AuctionCardDto();
                        dto.name = (item != null) ? item.getName() : "Sản phẩm #" + auction.getItemId();
                        dto.image = (item != null) ? item.getImagePath() : null;
                        dto.description = (item != null) ? item.getDescription() : "Không có mô tả.";
                        if (item != null) {
                            try {
                                Request sellerReq = new Request(MessageType.GET_ACCOUNT_BY_ID, item.getOwnerId());
                                Response sellerResp = ClientSocket.getInstance().sendRequest(sellerReq);
                                Account seller = (sellerResp != null && sellerResp.isSuccess()) ? (Account) sellerResp.getData() : null;
                                dto.sellerName = (seller != null) ? seller.getUsername() : "Người bán #" + item.getOwnerId();
                            } catch (Exception ex) {
                                dto.sellerName = "Người bán #" + item.getOwnerId();
                            }
                        } else {
                            dto.sellerName = "Ẩn danh";
                        }
                        dto.startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                        dto.endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";

                        long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                        dto.time = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
                        dto.price = String.format("%,.0f VNĐ", auction.getCurrentPrice());
                        dto.auction = auction;
                        System.out.println("DEBUG dto.auction.getId()=" + auction.getId());

                        dataList.add(dto);
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi lấy dữ liệu mạng của auction_id=" + auction.getId() + ": " + e.getMessage());
                    }
                }
                return dataList;
            }
        };

        // 🌟 XỬ LÝ KHI LUỒNG NGẦM CHẠY XONG THÀNH CÔNG (Đã về luồng chính JavaFX Application Thread)
        loadTask.setOnSucceeded(event -> {
            List<AuctionCardDto> results = loadTask.getValue();
            if (results == null || results.isEmpty()) {
                showEmptyState(true);
                return;
            }

            showEmptyState(false);

            // Duyệt danh sách data sạch, nạp file FXML dựng giao diện cực kỳ an toàn
            for (AuctionCardDto dto : results) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                    Node card = loader.load();
                    ProductCardController controller = loader.getController();

                    // Đổ dữ liệu thô lên Controller của Card
                    if (controller != null) {
                        String statusText = "Đang diễn ra";
                        if (dto.auction.getEndTime() != null && LocalDateTime.now().isAfter(dto.auction.getEndTime())) {
                            statusText = "Đã kết thúc";
                        }
                        controller.setData(dto.name, dto.price, statusText, dto.image, dto.description,
                                dto.sellerName, dto.startTimeStr, dto.endTimeStr);
                        controller.setOriginProductData(dto.auction);
                    }

                    // Thêm card thẳng vào container hiển thị
                    cardsContainer.getChildren().add(card);
                    card.setOnMouseClicked(e -> {
                        if (dto.auction == null) return;
                        new Thread(() -> {
                            try {
                                Response res = ClientSocket.getInstance().sendRequest(
                                        new Request(MessageType.GET_AUCTION_BY_ID, dto.auction.getId()));
                                if (res != null && res.isSuccess()) {
                                    Auction full = (Auction) res.getData();
                                    if (full != null) {
                                        Platform.runLater(() -> {
                                            try {
                                                FXMLLoader detailLoader = new FXMLLoader(getClass().getResource("/view/AuctionDetailView.fxml"));
                                                Parent detailView = detailLoader.load();
                                                AuctionDetailController detailController = detailLoader.getController();
                                                full.setSellerName(dto.sellerName);
                                                full.setDescription(dto.description);
                                                if (detailController != null) detailController.loadProductDetail(full);

                                                Parent root = card.getScene().getRoot();
                                                Node center = root.lookup("#contentArea");
                                                if (center instanceof StackPane contentArea) {
                                                    contentArea.getChildren().setAll(detailView);
                                                }
                                            } catch (Exception ex) {
                                                System.err.println("Lỗi: " + ex.getMessage());
                                            }
                                        });
                                    }
                                }
                            } catch (Exception ex) {
                                System.err.println("Lỗi mở chi tiết: " + ex.getMessage());
                            }
                        }).start();
                    });
                } catch (Exception e) {
                    System.err.println("❌ Lỗi dựng giao diện Card từ FXML: " + e.getMessage());
                }
            }
        });

        // Xử lý khi luồng ngầm bị lỗi (Mất mạng, nghẽn đường truyền...)
        loadTask.setOnFailed(event -> {
            Throwable e = loadTask.getException();
            System.err.println("❌ Lỗi luồng ngầm ActiveAuctions: " + e.getMessage());
            showEmptyState(true);
        });

        // Kích hoạt chạy luồng ngầm độc lập
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

    /**
     * 🚀 SỬA LỖI BIÊN DỊCH TRIỆT ĐỂ: Điều hướng quay lại trang chủ bằng kỹ thuật Scene Graph Lookup
     * Loại bỏ hoàn toàn sự phụ thuộc vào Singleton static cũ để tránh lỗi compile và rò rỉ RAM.
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
}
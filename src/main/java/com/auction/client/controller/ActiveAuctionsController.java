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
import javafx.scene.layout.FlowPane;
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

                List<Auction> auctions = (List<Auction>) response.getData();
                if (auctions == null || auctions.isEmpty()) return dataList;

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
                        dto.sellerName = (item != null) ? "Người bán #" + item.getOwnerId() : "Ẩn danh";

                        dto.startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
                        dto.endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";

                        long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
                        dto.time = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
                        dto.price = String.format("%,.0f VNĐ", auction.getCurrentPrice());

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
                    controller.setData(dto.name, dto.price, dto.time, dto.image, dto.description,
                            dto.sellerName, dto.startTimeStr, dto.endTimeStr);

                    // Thêm card thẳng vào container hiển thị
                    cardsContainer.getChildren().add(card);
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

    @FXML
    private void openHome() {
        if (MainLayoutController.getInstance() != null) {
            Platform.runLater(() -> {
                MainLayoutController.getInstance().openHome();
            });
        }
    }
}
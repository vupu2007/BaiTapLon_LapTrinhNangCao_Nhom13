package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.client.service.AuctionService.AuctionCardDto;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActiveAuctionsController {

    private static final Logger LOGGER = Logger.getLogger(ActiveAuctionsController.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Tiêm (Inject) Service vào để dùng
    private final AuctionService auctionService = new AuctionService();

    @FXML private VBox emptyStateBox;
    @FXML private FlowPane cardsContainer;

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

    private void changeCenterView(Node sourceNode, String fxmlPath, java.util.function.Consumer<FXMLLoader> controllerInitializer) {
        if (sourceNode == null || sourceNode.getScene() == null) return;
        try {
            java.net.URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) return;

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent view = loader.load();

            if (controllerInitializer != null) {
                controllerInitializer.accept(loader);
            }

            Parent root = sourceNode.getScene().getRoot();
            Node layoutCenter = root.lookup("#contentArea");
            if (layoutCenter instanceof StackPane contentArea) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi chuyển màn hình: " + fxmlPath, e);
        }
    }

    @FXML
    private void openHome() {
        Platform.runLater(() -> changeCenterView(cardsContainer, "/view/MainView.fxml", null));
    }

    private void loadAuctions() {
        if (cardsContainer != null) cardsContainer.getChildren().clear();

        // Task bây giờ siêu ngắn vì chỉ việc gọi Service
        Task<List<AuctionCardDto>> loadTask = new Task<>() {
            @Override
            protected List<AuctionCardDto> call() throws Exception {
                // Gọi sang tầng Service xử lý logic và mạng
                return auctionService.getActiveAuctionsByCurrentBidder();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<AuctionCardDto> results = loadTask.getValue();
            if (results == null || results.isEmpty()) {
                showEmptyState(true);
                return;
            }
            showEmptyState(false);
            results.forEach(this::createCardNode);
        });

        loadTask.setOnFailed(event -> {
            LOGGER.log(Level.SEVERE, "❌ Lỗi luồng ngầm", loadTask.getException());
            showEmptyState(true);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void createCardNode(AuctionCardDto dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
            Node card = loader.load();
            ProductCardController controller = loader.getController();

            if (controller != null) {
                String statusText = (dto.auction.getEndTime() != null && LocalDateTime.now().isAfter(dto.auction.getEndTime()))
                        ? "Đã kết thúc" : "Đang diễn ra";
                controller.setData(dto.name, dto.price, statusText, dto.image, dto.description,
                        dto.sellerName, dto.startTimeStr, dto.endTimeStr);
            }

            cardsContainer.getChildren().add(card);
            card.setOnMouseClicked(e -> navigateToDetail(card, dto));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi render card", e);
        }
    }

    private void navigateToDetail(Node card, AuctionCardDto dto) {
        if (dto.auction == null) return;

        // Đồng bộ hóa dữ liệu trước khi ném sang màn hình chi tiết
        dto.auction.setProductName(dto.name);
        dto.auction.setSellerName(dto.sellerName);
        dto.auction.setDescription(dto.description);
        try {
            if (dto.startTimeStr != null) dto.auction.setStartTime(LocalDateTime.parse(dto.startTimeStr, DATE_TIME_FORMATTER));
            if (dto.endTimeStr != null) dto.auction.setEndTime(LocalDateTime.parse(dto.endTimeStr, DATE_TIME_FORMATTER));
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Lỗi ép kiểu Date tại Controller", ex);
        }

        changeCenterView(card, "/view/AuctionDetail.fxml", loader -> {
            AuctionDetailController detailController = loader.getController();
            if (detailController != null) {
                detailController.loadProductDetail(dto.auction);
            }
        });
    }
}
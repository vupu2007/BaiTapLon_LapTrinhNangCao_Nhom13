package com.auction.client.controller;

import com.auction.client.service.MainDashboardService;
import com.auction.client.util.CurrentAccount;
import com.auction.client.util.ImageLoader;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {

    @FXML private Label balanceLabel, ongoingLabel, wonLabel, welcomeLabel;
    @FXML private Button btnFilterAll, btnFilterActive, btnFilterUpcoming;
    @FXML private FlowPane flowPane;

    private static MainController instance;
    private String currentFilter = "ALL";

    // Khởi tạo lớp tầng nghiệp vụ chuyên biệt phục vụ cấu trúc Enterprise
    private final MainDashboardService dashboardService = new MainDashboardService();

    public MainController() {}

    @FXML
    public void initialize() {
        instance = this;
        Account current = CurrentAccount.getAccount();
        if (current != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            Platform.runLater(this::refreshDashboard);
        }
    }

    public static MainController getInstance() {
        return instance;
    }

    /**
     * 🚀 ĐÃ CHUẨN HÓA: Ủy quyền lấy dữ liệu mạng hoàn toàn cho tầng Service
     */
    public void refreshDashboard() {
        Account current = CurrentAccount.getAccount();
        if (current == null) return;

        // Hiển thị số dư tiền mặt tài khoản
        if (balanceLabel != null) {
            balanceLabel.setText(current instanceof User
                    ? String.format("%.0f VND", ((User) current).getBalance()) : "N/A");
        }

        // Gọi dịch vụ Service chạy ngầm, nhận dữ liệu sạch thông qua cơ chế Callback lambda
        dashboardService.fetchDashboardDataAsync(current.getId(), currentFilter, (stats, items) -> {
            if (flowPane == null) return;

            // 1. Cập nhật các con số thống kê lên màn hình
            if (stats != null) {
                if (ongoingLabel != null) ongoingLabel.setText(String.valueOf(stats.getOrDefault("ongoing", 0)));
                if (wonLabel != null) wonLabel.setText(String.valueOf(stats.getOrDefault("won", 0)));
            }

            // 2. Kích hoạt hiệu ứng sáng tối cho các nút bộ lọc phân loại
            switch (currentFilter) {
                case "ALL" -> setButtonActive(btnFilterAll);
                case "ACTIVE" -> setButtonActive(btnFilterActive);
                case "UPCOMING" -> setButtonActive(btnFilterUpcoming);
            }

            // 3. Xóa giao diện cũ và vẽ loạt card sản phẩm mới tinh
            flowPane.getChildren().clear();
            String statusLabelText = currentFilter.equals("UPCOMING") ? "Sắp diễn ra" : "Đang diễn ra";

            for (Item item : items) {
                if (item != null) {
                    flowPane.getChildren().add(createItemCardWithStatus(item, statusLabelText));
                }
            }
            System.out.println("=== [UI] Đã hiển thị mượt mà " + flowPane.getChildren().size() + " thẻ đấu giá.");
        });
    }

    private void setButtonActive(Button activeButton) {
        Button[] filterButtons = {btnFilterAll, btnFilterActive, btnFilterUpcoming};
        for (Button btn : filterButtons) {
            if (btn != null)
                btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
        }
        if (activeButton != null)
            activeButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
    }

    @FXML private void handleFilterAll() { currentFilter = "ALL"; refreshDashboard(); }
    @FXML private void handleFilterActive() { currentFilter = "ACTIVE"; refreshDashboard(); }
    @FXML private void handleFilterUpcoming() { currentFilter = "UPCOMING"; refreshDashboard(); }

    /**
     * Dựng giao diện Card sản phẩm thủ công từ Item
     */
    private VBox createItemCardWithStatus(Item item, String statusText) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(300);
        imgView.setFitHeight(180);
        imgView.setPreserveRatio(false);

        String preferredName = item.getImagePath();
        if (preferredName == null || preferredName.trim().isEmpty()) {
            preferredName = "default.png";
            if (item instanceof Electronics) {
                String brand = ((Electronics) item).getBrand();
                if (brand != null && !brand.trim().isEmpty()) preferredName = brand.trim();
            }
        }

        // SỬA: Ủy quyền tải ảnh qua lớp tiện ích độc lập
        ImageLoader.tryLoadImageToView(imgView, preferredName);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 180);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgView.setClip(clip);
        imageHolder.getChildren().add(imgView);

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle(statusText.equals("Sắp diễn ra") ?
                "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-background-radius: 20; -fx-font-weight: bold;" :
                "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold格式;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));
        Label nameLabel = new Label(item.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));
        Label descLabel = new Label(item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "Sản phẩm chất lượng cao đang trong phiên đấu giá công khai.");
        descLabel.setPrefHeight(40);
        descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        descLabel.setWrapText(true);

        Region spacer = new Region();
        spacer.setPrefHeight(10);
        HBox priceBox = new HBox();
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá hiện tại:");
        priceTitle.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        Label priceValue = new Label(String.format("%,.0f đ", item.getStartingPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));
        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button(statusText.equals("Sắp diễn ra") ? "Xem chi tiết" : "Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0));
        bidButton.setOnAction(e -> showAuctionDetail(item));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    /**
     * Đồng bộ nhận gói tin ra giá Realtime đẩy từ Server
     */
    public void addAuctionToRealtimeUI(Auction newAuction) {
        Platform.runLater(() -> {
            if (flowPane != null && (currentFilter.equals("ALL") || currentFilter.equals("UPCOMING"))) {
                VBox card = createCardFromAuction(newAuction);
                flowPane.getChildren().add(0, card);
            }
        });
    }

    private VBox createCardFromAuction(Auction auction) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(300);
        imgView.setFitHeight(180);
        imgView.setPreserveRatio(false);

        String preferredName = auction.getImagePath();
        if (preferredName == null || preferredName.trim().isEmpty()) {
            preferredName = "default.png";
        }

        // Ủng quyền tiện ích tải ảnh ngoài
        ImageLoader.tryLoadImageToView(imgView, preferredName);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 180);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgView.setClip(clip);
        imageHolder.getChildren().add(imgView);

        Label statusLabel = new Label("Đang diễn ra");
        statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));
        Label nameLabel = new Label(auction.getProductName() != null ? auction.getProductName() : "Sản phẩm mới lên sàn");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));
        Label descLabel = new Label("Sản phẩm chất lượng cao đang trong phiên đấu giá công khai.");
        descLabel.setPrefHeight(40);
        descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        descLabel.setWrapText(true);

        Region spacer = new Region();
        spacer.setPrefHeight(10);
        HBox priceBox = new HBox();
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá hiện tại:");
        priceTitle.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        Label priceValue = new Label(String.format("%,.0f đ", auction.getStartPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));
        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0));
        bidButton.setOnAction(e -> showAuctionDetail(auction));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentAccount.logOut();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAuctionDetail(Object productData) {
        Platform.runLater(() -> {
            try {
                java.net.URL fxmlLocation = getClass().getResource("/view/AuctionDetailView.fxml");
                if (fxmlLocation == null) fxmlLocation = getClass().getResource("/view/AuctionDetail.fxml");
                if (fxmlLocation == null) fxmlLocation = getClass().getResource("/com/auction/client/view/AuctionDetailView.fxml");
                if (fxmlLocation == null) fxmlLocation = getClass().getResource("/com/auction/client/view/AuctionDetail.fxml");
                if (fxmlLocation == null) fxmlLocation = getClass().getResource("AuctionDetailView.fxml");

                if (fxmlLocation == null) {
                    System.err.println("❌ KHÔNG TÌM THẤY FILE FXML TRANG CHI TIẾT!");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Parent detailView = loader.load();
                AuctionDetailController detailController = loader.getController();

                if (detailController != null) {
                    if (productData instanceof Item) {
                        detailController.loadProductDetail((Item) productData);
                    } else if (productData instanceof Auction) {
                        detailController.loadProductDetail((Auction) productData);
                    }
                }

                if (MainLayoutController.getInstance() != null) {
                    MainLayoutController.getInstance().setContent(detailView);
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi nghiêm trọng khi tải trang chi tiết: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
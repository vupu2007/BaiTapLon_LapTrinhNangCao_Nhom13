package com.auction.client.controller;

import com.auction.server.service.MainService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.shared.model.Item;
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
import javafx.scene.layout.FlowPane; // ĐỔI THÀNH FLOWPANE
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label balanceLabel;
    @FXML private Label ongoingLabel;
    @FXML private Label wonLabel;
    @FXML private Label welcomeLabel;

    // SỬA CHỖ NÀY: Khớp hoàn toàn với fx:id="flowPane" trong file FXML trang chủ
    @FXML private FlowPane flowPane;

    private MainService mainService;
    private static MainController instance;

    public MainController() {
        this.mainService = new MainService();
    }

    @FXML
    public void initialize() {
        instance = this;

        Account current = CurrentAccount.getAccount();
        if (current != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            }
            refreshDashboard();
        }
    }

    public static MainController getInstance() {
        return instance;
    }

    /**
     * Hàm gọi từ Server/Socket hoặc CreateController khi tạo thành công.
     * Tự động render và đẩy sản phẩm mới lên đầu trang chủ ngay lập tức.
     */
    public void addAuctionToRealtimeUI(Auction newAuction) {
        Platform.runLater(() -> {
            if (flowPane != null) {
                // Tạo card đẹp mắt từ đối tượng Auction
                VBox card = createCardFromAuction(newAuction);
                // Chèn lên vị trí đầu tiên trong FlowPane
                flowPane.getChildren().add(0, card);
            }
        });
    }

    public void refreshDashboard() {
        Account current = CurrentAccount.getAccount();

        // THÊM ĐIỀU KIỆN KIỂM TRA != NULL CHO CÁC LABEL TRƯỚC KHI SET TEXT
        if (balanceLabel != null) {
            if (current instanceof User) {
                balanceLabel.setText(String.format("%.0f VNĐ", ((User) current).getBalance()));
            } else {
                balanceLabel.setText("N/A");
            }
        }

        if (ongoingLabel != null) {
            ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        }

        if (wonLabel != null) {
            wonLabel.setText(String.valueOf(mainService.getWonCount()));
        }

        loadHotAuctions();
    }

    private void loadHotAuctions() {
        if (flowPane == null) return;

        // Xóa sạch các phần tử cũ trước khi nạp mới
        flowPane.getChildren().clear();

        List<Item> items = mainService.getHotAuctions();
        for (Item item : items) {
            VBox card = createItemCard(item);
            flowPane.getChildren().add(card);
        }
    }

    /**
     * Hàm dựng Card từ thực thể Item (Dữ liệu cũ quét từ Database lúc khởi động)
     * Đã được nâng cấp giao diện bo góc, đổ bóng chuẩn như FXML của bạn
     */
    private VBox createItemCard(Item item) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        // Phần Header chứa ảnh/khối màu và Tag trạng thái
        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #262626; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        Label statusLabel = new Label("Đang diễn ra");
        statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        // Phần chi tiết văn bản
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));

        Label nameLabel = new Label(item.getName());
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

        Label priceValue = new Label(String.format("%,.0f đ", item.getStartingPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));

        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0));
        bidButton.setOnAction(e -> System.out.println("Đang đấu giá: " + item.getName()));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    /**
     * Hàm dựng Card từ thực thể Auction (Khi vừa nhận tạo thành công thời gian thực)
     */
    private VBox createCardFromAuction(Auction auction) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #4c1d95; -fx-background-radius: 11 11 0 0;"); // Màu tím đậm khác biệt cho phiên mới tạo
        imageHolder.getChildren().add(bgRegion);

        Label statusLabel = new Label("Mới tạo");
        statusLabel.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04; -fx-background-radius: 20; -fx-font-weight: bold;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));

        Label nameLabel = new Label("Sản phẩm: " + auction.getItemId());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));

        Label descLabel = new Label("Phiên đấu giá vừa được tạo thành công trên hệ thống.");
        descLabel.setPrefHeight(40);
        descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        descLabel.setWrapText(true);

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        HBox priceBox = new HBox();
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá khởi điểm:");
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
        bidButton.setOnAction(e -> System.out.println("Đang đấu giá sản phẩm: " + auction.getItemId()));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentAccount.logOut();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
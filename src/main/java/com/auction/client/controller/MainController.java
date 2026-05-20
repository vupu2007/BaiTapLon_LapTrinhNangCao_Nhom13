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
import javafx.scene.layout.FlowPane;
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
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterActive;
    @FXML private Button btnFilterUpcoming;
    @FXML private FlowPane flowPane;
    private MainService mainService;
    private static MainController instance;

    private String currentFilter = "ALL";

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

    public void addAuctionToRealtimeUI(Auction newAuction) {
        Platform.runLater(() -> {
            if (flowPane != null) {
                if (currentFilter.equals("ALL") || currentFilter.equals("UPCOMING")) {
                    VBox card = createCardFromAuction(newAuction);
                    flowPane.getChildren().add(0, card);
                }
            }
        });
    }

    public void refreshDashboard() {
        Account current = CurrentAccount.getAccount();

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

        handleFilterAll();
    }

    private void setButtonActive(Button activeButton) {
        Button[] filterButtons = {btnFilterAll, btnFilterActive, btnFilterUpcoming};
        for (Button btn : filterButtons) {
            if (btn != null) {
                btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
            }
        }
        if (activeButton != null) {
            activeButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
        }
    }

    @FXML
    private void handleFilterAll() {
        currentFilter = "ALL";
        setButtonActive(btnFilterAll);

        if (flowPane == null) return;
        flowPane.getChildren().clear();

        List<Item> items = mainService.getHotAuctions();
        for (Item item : items) {
            VBox card = createItemCardWithStatus(item, "Đang diễn ra");
            flowPane.getChildren().add(card);
        }
    }

    @FXML
    private void handleFilterActive() {
        currentFilter = "ACTIVE";
        setButtonActive(btnFilterActive);

        if (flowPane == null) return;
        flowPane.getChildren().clear();

        List<Item> items = mainService.getHotAuctions();
        for (Item item : items) {
            VBox card = createItemCardWithStatus(item, "Đang diễn ra");
            flowPane.getChildren().add(card);
        }
    }

    @FXML
    private void handleFilterUpcoming() {
        currentFilter = "UPCOMING";
        setButtonActive(btnFilterUpcoming);

        if (flowPane == null) return;
        flowPane.getChildren().clear();

        List<Item> items = mainService.getHotAuctions();
        for (Item item : items) {
            VBox card = createItemCardWithStatus(item, "Sắp diễn ra");
            flowPane.getChildren().add(card);
        }
    }

    private void _anchor_createItemCard(Item item) {
        // Hàm backup để không lỗi cấu trúc cũ
    }

    private VBox createItemCardWithStatus(Item item, String statusText) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #262626; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        Label statusLabel = new Label(statusText);
        if (statusText.equals("Sắp diễn ra")) {
            statusLabel.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold;");
        }
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

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

        Button bidButton = new Button(statusText.equals("Sắp diễn ra") ? "Xem chi tiết" : "Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0));

        bidButton.setOnAction(e -> {
            showAuctionDetail(item);
        });

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    /**
     * Hàm dựng Card từ thực thể Auction (Dữ liệu thời gian thực real-time)
     */
    private VBox createCardFromAuction(Auction auction) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #4c1d95; -fx-background-radius: 11 11 0 0;");
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

        // ĐÃ SỬA: Truyền trực tiếp đối tượng auction vào hàm showAuctionDetail
        bidButton.setOnAction(e -> {
            showAuctionDetail(auction);
        });

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

    /**
     * Hàm hiển thị chi tiết (Đã sửa đổi thông minh để nhận cả Item lẫn Auction)
     */
    public void showAuctionDetail(Object productData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AuctionDetail.fxml"));
            Parent detailView = loader.load();

            AuctionDetailController detailController = loader.getController();
            if (detailController != null) {
                if (productData instanceof Item) {
                    detailController.loadProductDetail((Item) productData);
                } else if (productData instanceof Auction) {
                    // Nếu là đối tượng phiên đấu giá real-time từ socket truyền vào
                    Auction auction = (Auction) productData;
                    // Tạo một hàm giả lập đổ tạm dữ liệu chuỗi vào controller chi tiết của bạn công khai
                    detailController.lblProductTitle.setText("Sản phẩm mã số #" + auction.getItemId());
                    detailController.lblStartPrice.setText(String.format("%.0f đ", auction.getStartPrice()));
                }
            }

            MainLayoutController layoutController = MainLayoutController.getInstance();
            if (layoutController != null) {
                layoutController.setContent(detailView);
            } else {
                System.err.println("LỖI: MainLayoutController.getInstance() đang trả về NULL!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package com.auction.client.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import com.auction.client.util.CurrentAccount;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainLayoutController {

    private static MainLayoutController instance;

    @FXML private StackPane contentArea;
    @FXML private VBox buyerMenu;
    @FXML private VBox sellerMenu;
    @FXML private VBox roleBox;
    @FXML private Label lblRoleSidebar;
    @FXML private MenuButton roleMenuButton;
    @FXML private Label lblClock;

    // ================= BUTTON MENU =================
    @FXML private Button btnHome;
    @FXML private Button btnWallet;
    @FXML private Button btnAuction;
    @FXML private Button btnSelling;
    @FXML private Button btnCreateAuction;
    @FXML private Button btnHistory;
    @FXML private Button btnSettings;

    @FXML private Label nameLabel;

    // Quản lý vòng đời luồng hệ thống chống rò rỉ RAM
    private Timeline clockTimeline;

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        if (CurrentAccount.getAccount() != null) {
            nameLabel.setText("👤 " + CurrentAccount.getAccount().getUsername());
        }
        startRealtimeClock();

        // Tự động mở trang chủ ngay khi giao diện cha sẵn sàng
        Platform.runLater(this::openHome);
    }

    private void startRealtimeClock() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        clockTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String currentTime = LocalDateTime.now().format(formatter);
                    if (lblClock != null) {
                        lblClock.setText(currentTime);
                    }
                })
        );
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * 🚀 TỐI ƯU HÀM TẢI TRANG: Tự động co giãn kích thước theo Panel cha
     */
    private FXMLLoader loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();

                if (page instanceof Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                    region.setMaxWidth(Double.MAX_VALUE);
                    region.setMaxHeight(Double.MAX_VALUE);
                }
                StackPane.setAlignment(page, Pos.TOP_CENTER);
                contentArea.getChildren().add(page);
            }

            return loader;
        } catch (Exception e) {
            System.err.println("❌ Lỗi tải trang: " + fxmlPath + " -> " + e.getMessage());
            return null;
        }
    }

    /**
     * Đồng bộ đổi màu thanh Menu đang chọn chủ động một cách an toàn
     */
    private void setActive(Button activeButton) {
        Button[] buttons = {btnHome, btnWallet, btnAuction, btnSelling, btnCreateAuction, btnHistory, btnSettings};
        for (Button btn : buttons) {
            if (btn != null && btn.getStyleClass() != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
            }
        }
        if (activeButton != null && activeButton.getStyleClass() != null) {
            activeButton.getStyleClass().remove("nav-button");
            if (!activeButton.getStyleClass().contains("nav-button-active")) {
                activeButton.getStyleClass().add("nav-button-active");
            }
        }
    }

    // ================= CHUYỂN TRANG CHI TIẾT ĐẤU GIÁ =================

    /**
     * Tìm đường dẫn FXML chuẩn xác trước khi ra lệnh nạp trang để tránh lỗi đè Node giao diện
     */
    private FXMLLoader loadAuctionDetailPage() {
        String primaryPath = "/view/AuctionDetailView.fxml";
        URL location = getClass().getResource(primaryPath);
        if (location == null) {
            primaryPath = "/view/AuctionDetail.fxml";
        }
        return loadPage(primaryPath);
    }

    public void openAuctionDetailWithObject(com.auction.shared.model.Auction auction) {
        if (auction == null) return;

        Platform.runLater(() -> {
            FXMLLoader loader = loadAuctionDetailPage();
            setActive(btnAuction);

            if (loader != null) {
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    detailController.loadProductDetail(auction);
                    System.out.println("✅ Đã chuyển tiếp đối tượng Đấu giá (Auction) sang trang chi tiết.");
                }
            }
        });
    }

    public void openAuctionDetailWithObject(com.auction.shared.model.Item item) {
        if (item == null) return;

        Platform.runLater(() -> {
            FXMLLoader loader = loadAuctionDetailPage();
            setActive(btnAuction);

            if (loader != null) {
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    detailController.loadProductDetail(item);
                    System.out.println("✅ Đã chuyển tiếp đối tượng Vật phẩm (Item) sang trang chi tiết.");
                }
            }
        });
    }

    public void openAuctionDetail(String name, String price, javafx.scene.image.Image fxImage, String imageFileName,
                                  String description, String sellerName, String startTime, String endTime) {
        Platform.runLater(() -> {
            com.auction.shared.model.Auction mockAuction = new com.auction.shared.model.Auction();
            mockAuction.setProductName(name);

            try {
                String cleanPrice = price.replaceAll("[^0-9]", "");
                if (!cleanPrice.isEmpty()) {
                    mockAuction.setCurrentPrice(Double.parseDouble(cleanPrice));
                    mockAuction.setStartPrice(Double.parseDouble(cleanPrice));
                }
            } catch (Exception e) {
                mockAuction.setStartPrice(0.0);
            }

            mockAuction.setStatus(com.auction.shared.model.Auction.AuctionStatus.RUNNING);
            mockAuction.setDescription(description);
            mockAuction.setImagePath(imageFileName);

            com.auction.shared.model.Seller sellerAccount = new com.auction.shared.model.Seller("", sellerName, "", "", 0.0);
            mockAuction.setAccount(sellerAccount);

            openAuctionDetailWithObject(mockAuction);
        });
    }

    // ================= CÁC SỰ KIỆN MENU ĐIỀU HƯỚNG SẠCH SẼ =================

    @FXML
    public void openHome() {
        loadPage("/view/MainView.fxml");
        setActive(btnHome);
    }

    @FXML
    public void openSelling() {
        loadPage("/view/MyProducts.fxml");
        setActive(btnSelling);
    }

    @FXML private void openWallet() { Platform.runLater(() -> { loadPage("/view/WalletView.fxml"); setActive(btnWallet); }); }
    @FXML private void openAuction() { Platform.runLater(() -> { loadPage("/view/ActiveAuctions.fxml"); setActive(btnAuction); }); }
    @FXML private void openCreateAuction() { Platform.runLater(() -> { loadPage("/view/CreateAuction.fxml"); setActive(btnCreateAuction); }); }
    @FXML private void openHistory() { Platform.runLater(() -> { loadPage("/view/HistoryView.fxml"); setActive(btnHistory); }); }
    @FXML private void openSettings() { Platform.runLater(() -> { loadPage("/view/Settings.fxml"); setActive(btnSettings); }); }

    public void showCreateProductView() { openCreateAuction(); }

    // ================= CẤU HÌNH PHÂN QUYỀN VAI TRÒ GIAO DIỆN =================

    @FXML
    private void switchToBuyer() {
        Platform.runLater(() -> {
            if (lblRoleSidebar != null) {
                lblRoleSidebar.setText("🛒 Người mua");
                lblRoleSidebar.setStyle("-fx-text-fill: #86198f; -fx-font-size: 18; -fx-font-weight: bold;");
            }
            if (roleMenuButton != null) {
                roleMenuButton.setText("🔄 Người mua");
                roleMenuButton.setStyle("-fx-background-color: #A21CAF; -fx-background-radius: 10; -fx-text-fill: white;");
            }
            if (roleBox != null) {
                roleBox.setStyle("-fx-background-color: #fae8ff; -fx-background-radius: 15; -fx-padding: 20;");
            }
            if (buyerMenu != null) { buyerMenu.setVisible(true); buyerMenu.setManaged(true); }
            if (sellerMenu != null) { sellerMenu.setVisible(false); sellerMenu.setManaged(false); }
        });
    }

    @FXML
    private void switchToSeller() {
        Platform.runLater(() -> {
            if (lblRoleSidebar != null) {
                lblRoleSidebar.setText("🏪 Người bán");
                lblRoleSidebar.setStyle("-fx-text-fill: #1d4ed8; -fx-font-size: 18; -fx-font-weight: bold;");
            }
            if (roleMenuButton != null) {
                roleMenuButton.setText("🔄 Người bán");
                roleMenuButton.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 10; -fx-text-fill: white;");
            }
            if (roleBox != null) {
                roleBox.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 15; -fx-padding: 20;");
            }
            if (sellerMenu != null) { sellerMenu.setVisible(true); sellerMenu.setManaged(true); }
            if (buyerMenu != null) { buyerMenu.setVisible(false); buyerMenu.setManaged(false); }
        });
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // 🌟 CRITICAL FIX: Giải phóng bộ nhớ, dập tắt luồng đồng hồ chạy ngầm hoàn toàn trước khi chuyển Scene
        if (clockTimeline != null) {
            clockTimeline.stop();
        }

        try {
            CurrentAccount.setAccount(null);
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập");
        } catch (IOException e) {
            System.err.println("❌ Lỗi đăng xuất hệ thống: " + e.getMessage());
        }
    }

    public void setContent(Node content) {
        if (contentArea != null && content != null) {
            Platform.runLater(() -> contentArea.getChildren().setAll(content));
        }
    }
}
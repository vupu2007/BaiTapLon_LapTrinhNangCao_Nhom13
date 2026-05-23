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

    @FXML
    public void initialize() {
        instance = this;
        if (CurrentAccount.getAccount() != null) {
            nameLabel.setText("👤 " + CurrentAccount.getAccount().getUsername());
        }
        startRealtimeClock();
        openHome();
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    private void startRealtimeClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline clockTimeline = new Timeline(
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

    private FXMLLoader loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().clear();

            if (page instanceof Region region) {
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }
            StackPane.setAlignment(page, Pos.TOP_CENTER);
            contentArea.getChildren().add(page);

            return loader;
        } catch (Exception e) {
            System.err.println("Lỗi tải trang: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    private void setActive(Button activeButton) {
        Button[] buttons = {btnHome, btnWallet, btnAuction, btnSelling, btnCreateAuction, btnHistory, btnSettings};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button");
            if (!activeButton.getStyleClass().contains("nav-button-active")) {
                activeButton.getStyleClass().add("nav-button-active");
            }
        }
    }

    /**
     * Khởi tạo thông tin chi tiết cuộc đấu giá và điều hướng sang giao diện hiển thị
     */
    public void openAuctionDetail(String name, String price, javafx.scene.image.Image fxImage, String imageFileName,
                                  String description, String sellerName, String startTime, String endTime) {

        Platform.runLater(() -> {
            FXMLLoader loader = loadPage("/view/AuctionDetailView.fxml");
            if (loader == null) loader = loadPage("/view/AuctionDetail.fxml");

            setActive(btnAuction);

            if (loader != null) {
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    com.auction.shared.model.Auction mockAuction = new com.auction.shared.model.Auction();
                    mockAuction.setProductName(name);

                    // Trích xuất thông tin giá từ chuỗi ký tự dữ liệu
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

                    // Đồng bộ định dạng thời gian
                    LocalDateTime start = null;
                    LocalDateTime end = null;
                    DateTimeFormatter[] formatters = {
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    };

                    if (startTime != null && !startTime.trim().isEmpty() && !startTime.contains("--")) {
                        for (DateTimeFormatter fmt : formatters) {
                            try { start = LocalDateTime.parse(startTime.trim(), fmt); break; } catch (Exception ignored) {}
                        }
                    }
                    if (endTime != null && !endTime.trim().isEmpty() && !endTime.contains("--")) {
                        for (DateTimeFormatter fmt : formatters) {
                            try { end = LocalDateTime.parse(endTime.trim(), fmt); break; } catch (Exception ignored) {}
                        }
                    }

                    mockAuction.setStartTime(start != null ? start : LocalDateTime.now());
                    mockAuction.setEndTime(end != null ? end : LocalDateTime.now().plusHours(2));

                    if (imageFileName != null) {
                        String cleanImageName = imageFileName.replaceAll("(?i)\\.(png|jpg|jpeg|gif)$", "");
                        mockAuction.setItemId(cleanImageName);
                    } else {
                        mockAuction.setItemId("default");
                    }

                    // Khởi tạo tài khoản người bán với cấu hình constructor 5 tham số định dạng chính xác
                    String finalSellerName = (sellerName != null && !sellerName.trim().isEmpty()) ? sellerName : "Ẩn danh";
                    com.auction.shared.model.Seller sellerAccount = new com.auction.shared.model.Seller("", finalSellerName, "", "", 0.0);
                    mockAuction.setAccount(sellerAccount);

                    // Nạp dữ liệu vào giao diện chi tiết cuộc đấu giá
                    detailController.loadProductDetail(mockAuction);

                    if (detailController.lblInfoDescription != null && description != null) {
                        detailController.lblInfoDescription.setText(description);
                    }
                }
            }
        });
    }

    /**
     * Điều hướng sang trang chi tiết bằng cách truyền trực tiếp đối tượng Auction
     */
    public void openAuctionDetailWithObject(com.auction.shared.model.Auction auction) {
        if (auction == null) return;

        Platform.runLater(() -> {
            FXMLLoader loader = loadPage("/view/AuctionDetailView.fxml");
            if (loader == null) loader = loadPage("/view/AuctionDetail.fxml");

            setActive(btnAuction);

            if (loader != null) {
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    detailController.loadProductDetail(auction);
                    System.out.println("✅ Hệ thống đã chuyển tiếp thông tin đối tượng Đấu giá thành công.");
                }
            }
        });
    }

    /**
     * Điều hướng sang trang chi tiết bằng cách truyền trực tiếp đối tượng Item
     */
    public void openAuctionDetailWithObject(com.auction.shared.model.Item item) {
        if (item == null) return;

        Platform.runLater(() -> {
            FXMLLoader loader = loadPage("/view/AuctionDetailView.fxml");
            if (loader == null) loader = loadPage("/view/AuctionDetail.fxml");

            setActive(btnAuction);

            if (loader != null) {
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    detailController.loadProductDetail(item);
                    System.out.println("✅ Hệ thống đã chuyển tiếp thông tin đối tượng Sản phẩm thành công.");
                }
            }
        });
    }

    @FXML
    public void openHome() {
        FXMLLoader loader = loadPage("/view/MainView.fxml");
        setActive(btnHome);

        if (loader != null) {
            // Sử dụng kỹ thuật Reflection để cập nhật trang chủ một cách an toàn và tối ưu độc lập
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method refreshMethod = controller.getClass().getMethod("refreshDashboard");
                    refreshMethod.invoke(controller);
                } catch (Exception e) {
                    System.out.println("ℹ️ Không thực thi phương thức refreshDashboard() tại View chính.");
                }
            }
        }
    }

    @FXML private void openWallet() { loadPage("/view/WalletView.fxml"); setActive(btnWallet); }
    @FXML private void openAuction() { loadPage("/view/ActiveAuctions.fxml"); setActive(btnAuction); }

    @FXML
    public void openSelling() {
        FXMLLoader loader = loadPage("/view/MyProducts.fxml");
        setActive(btnSelling);

        if (loader != null) {
            Object myProductsController = loader.getController();
            if (myProductsController != null) {
                try {
                    java.lang.reflect.Method loadMethod = myProductsController.getClass().getMethod("loadMyProductsData");
                    loadMethod.invoke(myProductsController);
                } catch (Exception e) {
                    System.out.println("ℹ️ Không tìm thấy phương thức loadMyProductsData().");
                }
            }
        }
    }

    @FXML private void openCreateAuction() { loadPage("/view/CreateAuction.fxml"); setActive(btnCreateAuction); }
    @FXML private void openHistory() { loadPage("/view/HistoryView.fxml"); setActive(btnHistory); }
    @FXML private void openSettings() { loadPage("/view/Settings.fxml"); setActive(btnSettings); }
    public void showCreateProductView() { openCreateAuction(); }

    @FXML
    private void switchToBuyer() {
        lblRoleSidebar.setText("🛒 Người mua");
        roleMenuButton.setText("🔄 Người mua");
        roleBox.setStyle("-fx-background-color: #fae8ff; -fx-background-radius: 15; -fx-padding: 20;");
        lblRoleSidebar.setStyle("-fx-text-fill: #86198f; -fx-font-size: 18; -fx-font-weight: bold;");
        roleMenuButton.setStyle("-fx-background-color: #A21CAF; -fx-background-radius: 10; -fx-text-fill: white;");
        buyerMenu.setVisible(true); buyerMenu.setManaged(true);
        sellerMenu.setVisible(false); sellerMenu.setManaged(false);
    }

    @FXML
    private void switchToSeller() {
        lblRoleSidebar.setText("🏪 Người bán");
        roleMenuButton.setText("🔄 Người bán");
        roleBox.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 15; -fx-padding: 20;");
        lblRoleSidebar.setStyle("-fx-text-fill: #1d4ed8; -fx-font-size: 18; -fx-font-weight: bold;");
        roleMenuButton.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 10; -fx-text-fill: white;");
        sellerMenu.setVisible(true); sellerMenu.setManaged(true);
        buyerMenu.setVisible(false); buyerMenu.setManaged(false);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            CurrentAccount.setAccount(null);
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setContent(Node content) {
        contentArea.getChildren().setAll(content);
    }
}
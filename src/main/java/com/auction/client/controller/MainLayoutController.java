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
import javafx.scene.image.Image; // 🔥 Đã thêm import để nhận Object Image trực tiếp

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

    // 🕒 Biến ánh xạ nhãn hiển thị đồng hồ thời gian thực từ FXML sang
    @FXML private Label lblClock;

    // ================= BUTTON MENU =================
    @FXML private Button btnHome;
    @FXML private Button btnWallet;
    @FXML private Button btnAuction;
    @FXML private Button btnSelling;
    @FXML private Button btnCreateAuction;
    @FXML private Button btnHistory;
    @FXML private Button btnSettings;

    // ================= INIT =================
    @FXML private Label nameLabel;

    @FXML
    public void initialize() {
        // Gán instance bằng chính object này khi JavaFX khởi tạo layout
        instance = this;

        // 1. Cập nhật tên người dùng
        if (CurrentAccount.getAccount() != null) {
            nameLabel.setText("👤 " + CurrentAccount.getAccount().getUsername());
        }

        // 2. Kích hoạt kim giây đồng hồ chạy ngầm thời gian thực tế ngay khi nạp giao diện tổng
        startRealtimeClock();

        // TỰ ĐỘNG LOAD TRANG CHỦ KHI MỞ APP
        openHome();
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    // 🕒 Hàm xử lý chạy ngầm cập nhật đồng hồ mỗi giây một lần liên tục
    private void startRealtimeClock() {
        // Định dạng hiển thị Giờ:Phút:Giây (Ví dụ: 14:23:05)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Tạo Timeline lặp lại vô hạn, cứ mỗi 1 giây (Duration.seconds(1)) sẽ lấy giờ máy tính nạp vào UI
        Timeline clockTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String currentTime = LocalDateTime.now().format(formatter);
                    if (lblClock != null) {
                        lblClock.setText(currentTime); // Đổ chuỗi giờ thực tế lên Widget Sidebar
                    }
                })
        );

        // Đặt chế độ lặp vô hạn và bấm nút kích hoạt chạy
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    // ================= LOAD PAGE ĐỂ TRẢ VỀ FXMLLoader =================
    private FXMLLoader loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().clear();

            // Kiểm tra và ép giãn trang con
            if (page instanceof Region) {
                Region region = (Region) page;
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }
            StackPane.setAlignment(page, Pos.TOP_CENTER);
            contentArea.getChildren().add(page);

            return loader; // Trả loader về để lấy Controller khi cần
        } catch (Exception e) {
            System.err.println("Lỗi load file: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    // ================= ACTIVE BUTTON =================
    private void setActive(Button activeButton) {
        Button[] buttons = {
                btnHome, btnWallet, btnAuction, btnSelling,
                btnCreateAuction, btnHistory, btnSettings
        };
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
    // ================= 🔥 HÀM MỞ CHI TIẾT ĐẤU GIÁ SONG LUỒNG AN TOÀN TUYỆT ĐỐI =================
    public void openAuctionDetail(String name, String price, javafx.scene.image.Image fxImage, String imageFileName) {
        // 1. Nạp file FXML chi tiết đấu giá vào vùng giữa
        FXMLLoader loader = loadPage("/view/AuctionDetailView.fxml");

        // 2. Chuyển trạng thái sáng nút sang "Đang đấu giá" trên thanh điều hướng
        setActive(btnAuction);

        // 3. Đổ dữ liệu sang controller chi tiết với cơ chế cứu cánh thông minh
        if (loader != null) {
            AuctionDetailController detailController = loader.getController();
            if (detailController != null) {
                // Truyền cả object ảnh và tên/chuỗi base64 gốc để sơ cua
                detailController.initData(name, price, fxImage, imageFileName);
            }
        }
    }

    // ================= OPEN HOME AUTO REFRESH DỮ LIỆU =================
    @FXML
    public void openHome() {
        // 1. Load trang chủ lên màn hình như bình thường
        FXMLLoader loader = loadPage("/view/MainView.fxml");
        setActive(btnHome);

        // 2. Ép MainController chạy hàm quét lại Database ngay lập tức khi mở tab
        if (loader != null) {
            MainController mainController = loader.getController();
            if (mainController != null) {
                mainController.refreshDashboard();
            }
        }
    }

    // ================= MENU ACTIONS =================
    @FXML
    private void openWallet() {
        loadPage("/view/WalletView.fxml");
        setActive(btnWallet);
    }
    @FXML
    private void openAuction() {
        loadPage("/view/ActiveAuctions.fxml");
        setActive(btnAuction);
    }

    // Ép trang "Đang bán" tự động refresh kéo dữ liệu cá nhân mới nhất khi bấm menu trái
    @FXML
    public void openSelling() {
        FXMLLoader loader = loadPage("/view/MyProducts.fxml");
        setActive(btnSelling);

        if (loader != null) {
            MyProductsController myProductsController = loader.getController();
            if (myProductsController != null) {
                myProductsController.loadMyProductsData();
            }
        }
    }

    @FXML
    private void openCreateAuction() {
        loadPage("/view/CreateAuction.fxml");
        setActive(btnCreateAuction);
    }
    @FXML
    private void openHistory() {
        loadPage("/view/HistoryView.fxml");
        setActive(btnHistory);
    }
    @FXML
    private void openSettings() {
        loadPage("/view/Settings.fxml");
        setActive(btnSettings);
    }

    public void showCreateProductView() {
        openCreateAuction();
    }

    // ================= ROLE SWITCHING =================
    @FXML
    private void switchToBuyer() {
        lblRoleSidebar.setText("🛒 Người mua");
        roleMenuButton.setText("🔄 Người mua");

        roleBox.setStyle("-fx-background-color: #fae8ff; -fx-background-radius: 15; -fx-padding: 20;");
        lblRoleSidebar.setStyle("-fx-text-fill: #86198f; -fx-font-size: 18; -fx-font-weight: bold;");
        roleMenuButton.setStyle("-fx-background-color: #A21CAF; -fx-background-radius: 10; -fx-text-fill: white;");

        buyerMenu.setVisible(true);
        buyerMenu.setManaged(true);
        sellerMenu.setVisible(false);
        sellerMenu.setManaged(false);
    }

    @FXML
    private void switchToSeller() {
        lblRoleSidebar.setText("🏪 Người bán");
        roleMenuButton.setText("🔄 Người bán");

        roleBox.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 15; -fx-padding: 20;");
        lblRoleSidebar.setStyle("-fx-text-fill: #1d4ed8; -fx-font-size: 18; -fx-font-weight: bold;");
        roleMenuButton.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 10; -fx-text-fill: white;");

        sellerMenu.setVisible(true);
        sellerMenu.setManaged(true);
        buyerMenu.setVisible(false);
        buyerMenu.setManaged(false);
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
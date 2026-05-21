package com.auction.client.controller;

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

import java.io.IOException;

public class MainLayoutController {

    private static MainLayoutController instance;

    @FXML private StackPane contentArea;
    @FXML private VBox buyerMenu;
    @FXML private VBox sellerMenu;
    @FXML private VBox roleBox;
    @FXML private Label lblRoleSidebar;
    @FXML private MenuButton roleMenuButton;

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
        // CỐ ĐỊNH LỖI: Gán instance bằng chính object này khi JavaFX khởi tạo layout
        instance = this;

        // 1. Cập nhật tên người dùng
        if (CurrentAccount.getAccount() != null) {
            nameLabel.setText("👤 " + CurrentAccount.getAccount().getUsername());
        }
        // TỰ ĐỘNG LOAD TRANG CHỦ KHI MỞ APP
        openHome();
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    // ================= SỬA LẠI HÀM LOAD PAGE ĐỂ TRẢ VỀ FXMLLoader =================
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

    // ================= CẬP NHẬT: HÀM MỞ CHI TIẾT ĐẤU GIÁ TỪ THẺ SẢN PHẨM =================
    public void openAuctionDetail(String name, String price) {
        // 1. Nạp file FXML chi tiết đấu giá vào vùng giữa
        FXMLLoader loader = loadPage("/view/AuctionDetailView.fxml");

        // 2. Chuyển trạng thái sáng nút sang "Đang đấu giá" trên thanh điều hướng
        setActive(btnAuction);

        // 3. Đổ dữ liệu động sang controller của trang chi tiết
        if (loader != null) {
            AuctionDetailController detailController = loader.getController();
            if (detailController != null) {
                detailController.initData(name, price);
            }
        }
    }

    // ================= SỬA LẠI HÀM OPEN HOME ĐỂ AUTO REFRESH DỮ LIỆU =================
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
    @FXML
    private void openSelling() {
        loadPage("/view/MyProducts.fxml");
        setActive(btnSelling);
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

        // Quay về trang chủ khi chọn Người mua
        //openHome();
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

        // SỬA TẠI ĐÂY: Thay thế openSelling() bằng openHome() để Người bán cũng về trang chủ
        //openHome();
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
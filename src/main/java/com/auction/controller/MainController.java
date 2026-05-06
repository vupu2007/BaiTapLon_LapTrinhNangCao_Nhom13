package com.auction.controller;

import com.auction.model.Item;
import com.auction.model.User;
import com.auction.service.MainService;
import com.auction.util.CurrentUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label balanceLabel;
    @FXML private Label ongoingLabel;
    @FXML private Label wonLabel;
    @FXML private Label welcomeLabel; // Nên thêm một Label chào mừng trong FXML
    @FXML private VBox hotItemsContainer;

    private MainService mainService;

    public MainController() {
        this.mainService = new MainService();
    }

    @FXML
    public void initialize() {
        // Kiểm tra xem đã đăng nhập chưa trước khi load dữ liệu
        User current = CurrentUser.getUser();
        if (current != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            }
            refreshDashboard();
        } else {
            System.err.println("Cảnh báo: Truy cập trái phép vào Dashboard!");
        }
    }

    private void refreshDashboard() {
        // Cập nhật thống kê từ Service
        balanceLabel.setText(String.format("%.0f VNĐ", mainService.getBalance()));
        ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        wonLabel.setText(String.valueOf(mainService.getWonCount()));

        loadHotAuctions();
    }

    private void loadHotAuctions() {
        hotItemsContainer.getChildren().clear();
        List<Item> items = mainService.getHotAuctions();

        for (Item item : items) {
            VBox card = createItemCard(item);
            hotItemsContainer.getChildren().add(card);
        }
    }

    // Tách riêng hàm tạo Card để code sạch sẽ hơn
    private VBox createItemCard(Item item) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        Label name = new Label(item.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label price = new Label("Giá khởi điểm: " + String.format("%.0f", item.getStartingPrice()) + " VNĐ");
        price.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");

        // Xử lý sự kiện khi bấm nút đấu giá
        bidButton.setOnAction(e -> handleBidAction(item));

        card.getChildren().addAll(name, price, bidButton);
        return card;
    }

    private void handleBidAction(Item item) {
        System.out.println("User " + CurrentUser.getUser().getUsername() + " đang đấu giá món: " + item.getName());
        // Chỗ này sau này sẽ gọi sang BidService để xử lý đặt giá
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // 1. Xóa thông tin phiên đăng nhập
        CurrentUser.logOut();

        // 2. Chuyển hướng về trang Login
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Giữ nguyên kích thước Stage khi đăng xuất
            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập - Auction System");

        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình khi Logout: " + e.getMessage());
        }
    }
    @FXML
    private VBox sidebar;
    @FXML
    private Button btnHome, btnProducts, btnCreate, btnHistory;

    private boolean isCollapsed = false;

    @FXML
    private void toggleSidebar() {
        if (isCollapsed) {
            // MỞ RỘNG
            sidebar.setMinWidth(250);
            sidebar.setPrefWidth(250);

            btnHome.setText("🏠  Trang chủ");
            btnProducts.setText("📦  Sản phẩm của tôi");
            btnCreate.setText("➕  Tạo phiên đấu giá");
            btnHistory.setText("🕒  Lịch sử");

            isCollapsed = false;
        } else {
            // THU NHỎ
            sidebar.setMinWidth(70);
            sidebar.setPrefWidth(70);

            // Ẩn chữ, chỉ để lại Icon
            btnHome.setText("🏠");
            btnProducts.setText("📦");
            btnCreate.setText("➕");
            btnHistory.setText("🕒");

            isCollapsed = true;
        }
    }
}
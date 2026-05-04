package com.auction.controller;

import com.auction.model.Item;
import com.auction.service.MainService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;

import java.util.List;

public class MainController {

    // Các fx:id này phải khớp với id bạn đặt trong Scene Builder
    @FXML
    private Label balanceLabel;
    @FXML
    private Label ongoingLabel;
    @FXML
    private Label wonLabel;

    // Đây là nơi chứa danh sách sản phẩm (ví dụ một VBox bên trong ScrollPane)
    @FXML
    private VBox hotItemsContainer;

    private MainService mainService;

    // Hàm khởi tạo
    public MainController() {
        this.mainService = new MainService();
    }

    /**
     * Hàm này tự động chạy khi giao diện được tải lên
     */
    @FXML
    public void initialize() {
        refreshDashboard();
    }

    /**
     * Cập nhật toàn bộ dữ liệu trên màn hình chính
     */
    private void refreshDashboard() {
        // 1. Cập nhật các con số thống kê
        balanceLabel.setText(String.format("%.0f VNĐ", mainService.getBalance()));
        ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        wonLabel.setText(String.valueOf(mainService.getWonCount()));

        // 2. Hiển thị danh sách sản phẩm đấu giá
        loadHotAuctions();
    }

    private void loadHotAuctions() {
        // Xóa các item cũ nếu có
        hotItemsContainer.getChildren().clear();

        List<Item> items = mainService.getHotAuctions();

        for (Item item : items) {
            // Tạo một VBox nhỏ cho mỗi sản phẩm
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 5;");

            Label name = new Label(item.getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label price = new Label("Giá hiện tại: " + String.format("%.0f", item.getStartingPrice()) + " VNĐ");
            price.setStyle("-fx-text-fill: #27ae60;");

            Button bidButton = new Button("Đấu giá ngay");
            bidButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");

            card.getChildren().addAll(name, price, bidButton);

            // Thêm card vào danh sách hiển thị
            hotItemsContainer.getChildren().add(card);
        }
    }

    @FXML
    private void handleLogout() {
        mainService.logout();
        // Thêm logic chuyển màn hình về LoginView.fxml tại đây nếu cần
    }
}
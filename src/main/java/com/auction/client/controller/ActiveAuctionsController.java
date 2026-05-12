package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

public class ActiveAuctionsController {

    @FXML
    private VBox emptyStateBox;

    @FXML
    private TableView<Object> activeAuctionsTable; // Thay Object bằng Model của bạn sau này

    @FXML
    private TableColumn<Object, String> colProduct;

    @FXML
    public void initialize() {
        // Mặc định khi mới vào:
        // Nếu chưa có dữ liệu từ Database, hãy hiện emptyStateBox
        showEmptyState(true);

        // Sau này khi code Database xong,chỉ cần gọi:
        // if (list.isEmpty()) showEmptyState(true); else showEmptyState(false);
    }

    /**
     * Hàm dùng để chuyển đổi giao diện giữa lúc có dữ liệu và lúc trống
     */
    private void showEmptyState(boolean isEmpty) {
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);

        activeAuctionsTable.setVisible(!isEmpty);
        activeAuctionsTable.setManaged(!isEmpty);
    }

    @FXML
    private void openHome() {
        // Logic để quay lại trang chủ
        System.out.println("Đang quay lại trang chủ để khám phá sản phẩm...");

        // Nếu bạn muốn gọi hàm từ MainLayoutController,
        // bạn có thể sử dụng EventBus hoặc Dependency Injection.
    }
}
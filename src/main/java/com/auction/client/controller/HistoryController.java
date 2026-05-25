package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Map;

public class HistoryController {

    // --- KHAI BÁO UI ---
    @FXML private Label totalSessionsLabel;
    @FXML private Label wonSessionsLabel;
    @FXML private Label lostSessionsLabel;
    @FXML private Button btnHistory;

    private int currentUserId = 1;

    @FXML
    public void initialize() {
        loadHistoryStats();
    }

    private void loadHistoryStats() {
        if (CurrentAccount.getAccount() == null) return;
        int userId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // 🚀 Tách một luồng ngầm xử lý gọi Socket, giúp màn hình lịch sử hiển thị lên ngay lập tức mà không bị đơ
        Thread networkWorker = new Thread(() -> {
            try {
                Request request = new Request(MessageType.GET_BID_HISTORY_STATS, userId);
                Response response = ClientSocket.getInstance().sendRequest(request);

                // 🚀 Khi có dữ liệu trả về, đẩy việc hiển thị chữ lên Label về lại luồng UI an toàn
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        Map<String, Integer> stats = (Map<String, Integer>) response.getData();
                        totalSessionsLabel.setText(String.valueOf(stats.getOrDefault("total", 0)));
                        wonSessionsLabel.setText(String.valueOf(stats.getOrDefault("won", 0)));
                        lostSessionsLabel.setText(String.valueOf(stats.getOrDefault("lost", 0)));
                    } else {
                        setDefaultLabels();
                    }
                });
            } catch (Exception e) {
                System.err.println("❌ Lỗi lấy lịch sử chạy ngầm: " + e.getMessage());
                Platform.runLater(this::setDefaultLabels);
            }
        }, "HistoryStatsLoaderThread");

        networkWorker.setDaemon(true);
        networkWorker.start();
    }

    // Tiện ích reset nhãn về 0 khi có lỗi hoặc thất bại
    private void setDefaultLabels() {
        if (totalSessionsLabel != null) totalSessionsLabel.setText("0");
        if (wonSessionsLabel != null) wonSessionsLabel.setText("0");
        if (lostSessionsLabel != null) lostSessionsLabel.setText("0");
    }

    @FXML
    private void handleHistoryClick() {
        try {
            // Giữ nguyên logic chuyển trang của bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/HistoryView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnHistory.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang Lịch sử!");
        }
    }

    // Thêm hàm showAlert để không bị lỗi symbol 'showAlert' - Giữ nguyên
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
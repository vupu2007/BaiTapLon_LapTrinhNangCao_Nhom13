package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button; // Thêm import này
import javafx.scene.control.Alert;  // Thêm import này
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.auction.server.util.DatabaseConnection;
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

        try {
            Request request = new Request(MessageType.GET_BID_HISTORY_STATS, userId);
            Response response = ClientSocket.getInstance().sendRequest(request);

            if (response != null && response.isSuccess()) {
                Map<String, Integer> stats = (Map<String, Integer>) response.getData();
                totalSessionsLabel.setText(String.valueOf(stats.getOrDefault("total", 0)));
                wonSessionsLabel.setText(String.valueOf(stats.getOrDefault("won", 0)));
                lostSessionsLabel.setText(String.valueOf(stats.getOrDefault("lost", 0)));
            } else {
                totalSessionsLabel.setText("0");
                wonSessionsLabel.setText("0");
                lostSessionsLabel.setText("0");
            }
        } catch (Exception e) {
            System.err.println("Lỗi lấy lịch sử: " + e.getMessage());
        }
    }

    @FXML
    private void handleHistoryClick() {
        try {
            // Đảm bảo đường dẫn này chính xác trong thư mục resources của bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/HistoryView.fxml"));
            Parent root = loader.load();

            // Lấy stage từ bất kỳ node nào đã được khai báo @FXML (ở đây dùng btnHistory)
            Stage stage = (Stage) btnHistory.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang Lịch sử!");
        }
    }

    // Thêm hàm showAlert để không bị lỗi symbol 'showAlert'
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
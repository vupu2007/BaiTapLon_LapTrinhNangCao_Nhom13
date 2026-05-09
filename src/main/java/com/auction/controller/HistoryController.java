package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button; // Thêm import này
import javafx.scene.control.Alert;  // Thêm import này
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.auction.util.DatabaseConnection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

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
        String query = "SELECT " +
                "COUNT(*) AS total, " +
                "SUM(CASE WHEN status = 'WON' THEN 1 ELSE 0 END) AS won, " +
                "SUM(CASE WHEN status = 'LOST' THEN 1 ELSE 0 END) AS lost " +
                "FROM user_bids WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                totalSessionsLabel.setText(String.valueOf(rs.getInt("total")));
                wonSessionsLabel.setText(String.valueOf(rs.getInt("won")));
                lostSessionsLabel.setText(String.valueOf(rs.getInt("lost")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
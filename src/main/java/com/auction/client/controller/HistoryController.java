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
import java.net.URL;
import java.util.Map;

public class HistoryController {

    @FXML private Label totalSessionsLabel;
    @FXML private Label wonSessionsLabel;
    @FXML private Label lostSessionsLabel;
    @FXML private Button btnHistory;

    @FXML
    public void initialize() {
        loadHistoryStats();
    }

    private void loadHistoryStats() {
        // 🌟 SỬA: Nếu chưa đăng nhập, đưa nhãn về số 0 trước khi return để UI sạch sẽ
        if (CurrentAccount.getAccount() == null) {
            setDefaultLabels();
            return;
        }

        int userId = Integer.parseInt(CurrentAccount.getAccount().getId());

        Thread networkWorker = new Thread(() -> {
            try {
                Request request = new Request(MessageType.GET_BID_HISTORY_STATS, userId);
                Response response = ClientSocket.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        // 🌟 CRITICAL FIX: Ép kiểu thô (Wildcard) để tránh lỗi ClassCastException do sai lệch Long/Integer
                        Map<?, ?> stats = (Map<?, ?>) response.getData();

                        totalSessionsLabel.setText(String.valueOf(getSafeInt(stats.get("total"))));
                        wonSessionsLabel.setText(String.valueOf(getSafeInt(stats.get("won"))));
                        lostSessionsLabel.setText(String.valueOf(getSafeInt(stats.get("lost"))));
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

    /**
     * Tiện ích chuyển đổi dữ liệu an toàn từ Object sang int, chấp nhận cả Long/Double từ Server gửi về
     */
    private int getSafeInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return 0;
    }

    private void setDefaultLabels() {
        if (totalSessionsLabel != null) totalSessionsLabel.setText("0");
        if (wonSessionsLabel != null) wonSessionsLabel.setText("0");
        if (lostSessionsLabel != null) lostSessionsLabel.setText("0");
    }

    @FXML
    private void handleHistoryClick() {
        try {
            // 🌟 CHUẨN HÓA: Quét đường dẫn thông minh tránh lỗi sai cấu trúc thư mục tài nguyên giữa các máy
            String path = "/view/HistoryView.fxml";
            URL fxmlLocation = getClass().getResource(path);
            if (fxmlLocation == null) {
                // Thử đường dẫn dự phòng nếu cấu trúc của bạn nằm sâu trong package com.auction
                path = "/com/auction/view/HistoryView.fxml";
                fxmlLocation = getClass().getResource(path);
            }

            if (fxmlLocation == null) {
                throw new IOException("Không tìm thấy file HistoryView.fxml ở bất kỳ vị trí nào.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Stage stage = (Stage) btnHistory.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể khởi tạo màn hình chi tiết lịch sử!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}
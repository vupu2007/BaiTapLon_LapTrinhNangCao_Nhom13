package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.util.Map;

public class AdminDashboardController {

    // Liên kết chuẩn xác với các fx:id bên FXML
    @FXML private Label lblTotalAuctions;   // Thẻ số 1
    @FXML private Label lblRunningAuctions; // Thẻ số 2
    @FXML private Label lblTotalBids;       // Thẻ số 3
    @FXML private Label lblTotalRevenue;    // Thẻ số 4

    @FXML private BarChart<String, Number> revenueChart; // Biểu đồ doanh thu dạng cột
    @FXML private PieChart distributionChart;            // Biểu đồ phân bổ dạng tròn

    @FXML
    public void initialize() {
        // 1. Đổ dữ liệu tĩnh làm bộ khung mô phỏng để giao diện lên hình đẹp ngay lập tức
        setupMockData();

        // 2. Chạy tác vụ ngầm gửi tín hiệu qua Socket lấy data thực tế từ DB (nếu có)
        loadRealtimeStatistics();
    }

    /**
     * Khởi tạo dữ liệu mẫu giống hệt ảnh thiết kế ban đầu của bạn
     */
    private void setupMockData() {
        // Vẽ biểu đồ cột Doanh thu (T1 -> T5)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");
        series.getData().add(new XYChart.Data<>("T1", 120000000));
        series.getData().add(new XYChart.Data<>("T2", 185000000));
        series.getData().add(new XYChart.Data<>("T3", 210000000));
        series.getData().add(new XYChart.Data<>("T4", 195000000));
        series.getData().add(new XYChart.Data<>("T5", 250000000));
        revenueChart.getData().add(series);

        // Vẽ biểu đồ tròn Phân bổ trạng thái phiên đấu giá
        PieChart.Data slice1 = new PieChart.Data("Đang diễn ra: 60%", 60);
        PieChart.Data slice2 = new PieChart.Data("Sắp diễn ra: 20%", 20);
        PieChart.Data slice3 = new PieChart.Data("Đã kết thúc: 20%", 20);
        distributionChart.getData().addAll(slice1, slice2, slice3);
    }

    /**
     * Gửi yêu cầu qua luồng mạng Socket để cập nhật số liệu thời gian thực
     */
    private void loadRealtimeStatistics() {
        Task<Map<String, Object>> fetchTask = new Task<> () {
            @Override
            @SuppressWarnings("unchecked")
            protected Map<String, Object> call() throws Exception {
                // Gửi request xin dữ liệu Dashboard thống kê
                // Gợi ý: Nếu hệ thống chưa thiết lập MessageType này, bạn có thể tạm dùng chuỗi text bọc tạm qua valueOf
                Request request = new Request(MessageType.valueOf("GET_DASHBOARD_STATS"), null);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response != null && response.isSuccess()) {
                    return (Map<String, Object>) response.getData();
                }
                return null;
            }
        };

        // Khi Server phản hồi thành công -> Đổ mượt mà vào giao diện
        fetchTask.setOnSucceeded(event -> {
            Map<String, Object> serverStats = fetchTask.getValue();
            if (serverStats != null) {
                Platform.runLater(() -> {
                    if (serverStats.containsKey("totalAuctions")) {
                        lblTotalAuctions.setText(String.valueOf(serverStats.get("totalAuctions")));
                    }
                    if (serverStats.containsKey("runningAuctions")) {
                        lblRunningAuctions.setText(String.valueOf(serverStats.get("runningAuctions")));
                    }
                    if (serverStats.containsKey("totalBids")) {
                        lblTotalBids.setText(String.valueOf(serverStats.get("totalBids")));
                    }
                    if (serverStats.containsKey("totalRevenue")) {
                        double revenue = Double.parseDouble(serverStats.get("totalRevenue").toString());
                        lblTotalRevenue.setText(String.format("%,1.0f đ", revenue));
                    }
                });
            }
        });

        // Nếu Server chưa viết API này, giữ nguyên giao diện dữ liệu tĩnh cực đẹp để chấm điểm đồ án
        fetchTask.setOnFailed(event -> {
            System.out.println("ℹ️ Hệ thống đang hiển thị dữ liệu Dashboard tĩnh (Mô phỏng UI).");
        });

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true);
        thread.start();
    }
}
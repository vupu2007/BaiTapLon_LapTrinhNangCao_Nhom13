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

import com.auction.shared.model.Auction;
import java.util.List;

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
        new Thread(() -> {
            try {
                Response resp = ClientSocket.getInstance().sendRequest(
                        new Request(MessageType.GET_ALL_AUCTIONS, null));
                if (resp == null || !resp.isSuccess()) return;

                List<Auction> auctions = (List<Auction>) resp.getData();
                if (auctions == null) return;

                long total = auctions.size();
                long running = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.RUNNING).count();
                long finished = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.FINISHED).count();
                long open = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.OPEN).count();
                long canceled = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.CANCELED).count();
                double revenue = auctions.stream()
                        .filter(a -> a.getStatus() != Auction.AuctionStatus.CANCELED)
                        .mapToDouble(Auction::getCurrentPrice).sum();
                long totalBids = auctions.stream().mapToLong(Auction::getBidCount).sum();

                Platform.runLater(() -> {
                    if (lblTotalAuctions != null) lblTotalAuctions.setText(String.valueOf(total));
                    if (lblRunningAuctions != null) lblRunningAuctions.setText(String.valueOf(running));
                    if (lblTotalBids != null) lblTotalBids.setText(String.valueOf(totalBids));
                    if (lblTotalRevenue != null) lblTotalRevenue.setText(String.format("%,.0f đ", revenue));

                    distributionChart.getData().clear();
                    distributionChart.getData().addAll(
                            new PieChart.Data("Đang diễn ra: " + running, running),
                            new PieChart.Data("Sắp diễn ra: " + open, open),
                            new PieChart.Data("Đã kết thúc: " + finished, finished),
                            new PieChart.Data("Bị hủy: " + canceled, canceled)
                    );
                    revenueChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Doanh thu");
                    auctions.stream()
                            .filter(a -> a.getStatus() != Auction.AuctionStatus.CANCELED && a.getEndTime() != null)
                            .collect(java.util.stream.Collectors.groupingBy(
                                    a -> "T" + a.getEndTime().getMonthValue(),
                                    java.util.TreeMap::new,
                                    java.util.stream.Collectors.summingDouble(Auction::getCurrentPrice)))
                            .forEach((month, rev) -> series.getData().add(new XYChart.Data<>(month, rev)));
                    revenueChart.getData().add(series);
                });
            } catch (Exception e) {
                System.err.println("Lỗi load dashboard: " + e.getMessage());
            }
        }).start();

    }

}
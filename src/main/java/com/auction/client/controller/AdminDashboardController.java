package com.auction.client.controller;

import com.auction.client.service.AdminAuctionService;
import com.auction.shared.model.Auction;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.util.List;

public class AdminDashboardController {

    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRunningAuctions;
    @FXML private Label lblTotalBids;
    @FXML private Label lblTotalRevenue;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private PieChart distributionChart;

    private final AdminAuctionService auctionService = new AdminAuctionService();

    @FXML
    public void initialize() {
        setupMockData();
        loadRealtimeStatistics();
    }

    private void setupMockData() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");
        series.getData().add(new XYChart.Data<>("T1", 120000000));
        series.getData().add(new XYChart.Data<>("T2", 185000000));
        series.getData().add(new XYChart.Data<>("T3", 210000000));
        series.getData().add(new XYChart.Data<>("T4", 195000000));
        series.getData().add(new XYChart.Data<>("T5", 250000000));
        revenueChart.getData().add(series);

        distributionChart.getData().addAll(
                new PieChart.Data("Đang diễn ra: 60%", 60),
                new PieChart.Data("Sắp diễn ra: 20%", 20),
                new PieChart.Data("Đã kết thúc: 20%", 20)
        );
    }

    private void loadRealtimeStatistics() {
        // ✅ Controller không biết gì về Request/MessageType
        auctionService.fetchDashboardStatsAsync(auctions -> {
            if (auctions == null) return;

            long total    = auctions.size();
            long running  = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.RUNNING).count();
            long finished = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.FINISHED).count();
            long open     = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.OPEN).count();
            long canceled = auctions.stream().filter(a -> a.getStatus() == Auction.AuctionStatus.CANCELED).count();
            double revenue = auctions.stream()
                    .filter(a -> a.getStatus() != Auction.AuctionStatus.CANCELED)
                    .mapToDouble(Auction::getCurrentPrice).sum();
            long totalBids = auctions.stream().mapToLong(Auction::getBidCount).sum();

            if (lblTotalAuctions  != null) lblTotalAuctions.setText(String.valueOf(total));
            if (lblRunningAuctions != null) lblRunningAuctions.setText(String.valueOf(running));
            if (lblTotalBids      != null) lblTotalBids.setText(String.valueOf(totalBids));
            if (lblTotalRevenue   != null) lblTotalRevenue.setText(String.format("%,.0f đ", revenue));

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
                    .filter(a -> a.getStatus() != Auction.AuctionStatus.CANCELED
                            && a.getEndTime() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            a -> "T" + a.getEndTime().getMonthValue(),
                            java.util.TreeMap::new,
                            java.util.stream.Collectors.summingDouble(Auction::getCurrentPrice)))
                    .forEach((month, rev) ->
                            series.getData().add(new XYChart.Data<>(month, rev)));
            revenueChart.getData().add(series);
        });
    }
}
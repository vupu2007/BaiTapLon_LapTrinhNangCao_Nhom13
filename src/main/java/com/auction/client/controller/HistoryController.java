package com.auction.client.controller;

import com.auction.client.service.HistoryService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Map;

public class HistoryController {

    @FXML private Label totalSessionsLabel;
    @FXML private Label wonSessionsLabel;
    @FXML private Label lostSessionsLabel;

    @FXML private TableView<Auction> historyTable;
    @FXML private TableColumn<Auction, Integer> colId;
    @FXML private TableColumn<Auction, String> colProduct;
    @FXML private TableColumn<Auction, String> colDate;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;

    private final HistoryService historyService = new HistoryService();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadHistoryStats();
        loadHistoryTable();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colDate.setCellValueFactory(data -> {
            Auction a = data.getValue();
            String date = a.getEndTime() != null
                    ? a.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "--";
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%,.0f đ", price));
            }
        });
    }

    private void loadHistoryStats() {
        if (CurrentAccount.getAccount() == null) {
            setDefaultLabels(); return;
        }
        int userId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // ✅ Controller không biết gì về Request/MessageType
        historyService.fetchHistoryStatsAsync(userId, stats -> {
            if (stats != null) {
                totalSessionsLabel.setText(String.valueOf(getSafeInt(stats.get("total"))));
                wonSessionsLabel.setText(String.valueOf(getSafeInt(stats.get("won"))));
                lostSessionsLabel.setText(String.valueOf(getSafeInt(stats.get("lost"))));
            } else {
                setDefaultLabels();
            }
        });
    }

    private void loadHistoryTable() {
        if (CurrentAccount.getAccount() == null) return;
        int userId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // ✅ Controller không biết gì về Request/MessageType
        historyService.fetchHistoryTableAsync(userId, auctions -> {
            if (auctions != null) historyTable.getItems().setAll(auctions);
        });
    }

    @FXML
    private void handleRefresh() {
        historyTable.getItems().clear();
        loadHistoryStats();
        loadHistoryTable();
    }

    private int getSafeInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return 0;
    }

    private void setDefaultLabels() {
        if (totalSessionsLabel != null) totalSessionsLabel.setText("0");
        if (wonSessionsLabel != null) wonSessionsLabel.setText("0");
        if (lostSessionsLabel != null) lostSessionsLabel.setText("0");
    }
}
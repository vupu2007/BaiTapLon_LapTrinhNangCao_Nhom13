package com.auction.client.controller;

import com.auction.client.service.AdminItemService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Map;

public class AdminItemMgmtController {

    @FXML private TableView<AdminItemRow> itemTable;
    @FXML private TableColumn<AdminItemRow, String> colItemId;
    @FXML private TableColumn<AdminItemRow, String> colItemName;
    @FXML private TableColumn<AdminItemRow, String> colSeller;
    @FXML private TableColumn<AdminItemRow, String> colCurrentPrice;
    @FXML private TableColumn<AdminItemRow, String> colStatus;
    @FXML private TableColumn<AdminItemRow, Void> colAction;
    @FXML private TextField txtSearch;

    private final ObservableList<AdminItemRow> masterData = FXCollections.observableArrayList();
    private final AdminItemService itemService = new AdminItemService();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchFilter();
        loadItemDatabase();
    }

    private void setupTableColumns() {
        colItemId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colItemName.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());
        colSeller.setCellValueFactory(cellData -> cellData.getValue().sellerProperty());
        colCurrentPrice.setCellValueFactory(cellData -> cellData.getValue().currentPriceProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("OPENING".equalsIgnoreCase(item) || "ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                        setText("ĐANG ĐẤU GIÁ");
                    } else {
                        setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
                        setText("ĐÃ KẾT THÚC");
                    }
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Gỡ bỏ bài");
            {
                btnDelete.setPrefWidth(120);
                btnDelete.setPrefHeight(25);
                btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setOnAction(event -> {
                    AdminItemRow selected = getTableView().getItems().get(getIndex());
                    deleteViolatedItem(selected);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void loadItemDatabase() {
        masterData.clear();
        // ✅ Controller không biết gì về Request/MessageType
        itemService.fetchAllItemsAsync(items -> {
            if (items == null) return;
            for (Map<String, String> itemMap : items) {
                masterData.add(new AdminItemRow(
                        itemMap.get("id"),
                        itemMap.get("title"),
                        itemMap.get("sellerName"),
                        itemMap.get("currentPrice"),
                        itemMap.get("status")
                ));
            }
        });
    }

    private void deleteViolatedItem(AdminItemRow itemRow) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn gỡ bỏ sản phẩm [" + itemRow.getItemName() + "] không?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Xác nhận gỡ bài");
        confirm.showAndWait().ifPresent(type -> {
            if (type != ButtonType.YES) return;

            // ✅ Controller không biết gì về Request/MessageType
            itemService.deleteItemAsync(itemRow.getId(), resp -> {
                if (resp != null && resp.isSuccess()) {
                    masterData.remove(itemRow);
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Đã gỡ bài thành công!");
                } else {
                    String msg = resp != null ? resp.getMessage() : "Server từ chối.";
                    showAlert(Alert.AlertType.ERROR, "Thất bại",
                            "Không thể gỡ sản phẩm: " + msg);
                }
            });
        });
    }

    private void setupSearchFilter() {
        FilteredList<AdminItemRow> filteredData = new FilteredList<>(masterData, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(row -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                return row.getItemName().toLowerCase().contains(filter) ||
                        row.getSeller().toLowerCase().contains(filter);
            });
        });
        itemTable.setItems(filteredData);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── ROW MODEL ──────────────────────────────────
    public static class AdminItemRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty itemName;
        private final SimpleStringProperty seller;
        private final SimpleStringProperty currentPrice;
        private final SimpleStringProperty status;

        public AdminItemRow(String id, String itemName, String seller,
                            String currentPrice, String status) {
            this.id           = new SimpleStringProperty(id);
            this.itemName     = new SimpleStringProperty(itemName);
            this.seller       = new SimpleStringProperty(seller);
            this.currentPrice = new SimpleStringProperty(currentPrice);
            this.status       = new SimpleStringProperty(status);
        }

        public String getId() { return id.get(); }
        public SimpleStringProperty idProperty() { return id; }
        public String getItemName() { return itemName.get(); }
        public SimpleStringProperty itemNameProperty() { return itemName; }
        public String getSeller() { return seller.get(); }
        public SimpleStringProperty sellerProperty() { return seller; }
        public String getCurrentPrice() { return currentPrice.get(); }
        public SimpleStringProperty currentPriceProperty() { return currentPrice; }
        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
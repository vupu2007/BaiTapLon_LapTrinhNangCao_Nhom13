package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
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

    private ObservableList<AdminItemRow> masterData = FXCollections.observableArrayList();

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

        // Tô màu trạng thái phiên đấu giá (Đang diễn ra: Xanh dương, Đã kết thúc: Xám)
        colStatus.setCellFactory(column -> new TableCell<AdminItemRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
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

        // Cột Hành động: Nút gỡ bỏ sản phẩm vi phạm quy chế
        colAction.setCellFactory(param -> new TableCell<AdminItemRow, Void>() {
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
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                }
            }
        });
    }

    /**
     * Gửi yêu cầu gỡ sản phẩm lên Server bất đồng bộ qua Socket
     */
    private void deleteViolatedItem(AdminItemRow itemRow) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn gỡ bỏ sản phẩm [" + itemRow.getItemName() + "] vi phạm này không?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Xác nhận gỡ bài");
        confirm.showAndWait().ifPresent(responseType -> {
            if (responseType == ButtonType.YES) {
                // Đóng gói mảng chuỗi gửi lên Server xử lý (Định dạng: { itemId })
                String[] payload = { itemRow.getId() };
                Request request = new Request(MessageType.DELETE_ITEM, payload);

                Thread deleteWorker = new Thread(() -> {
                    try {
                        Response response = ClientSocket.getInstance().sendRequest(request);
                        Platform.runLater(() -> {
                            if (response != null && response.isSuccess()) {
                                masterData.remove(itemRow); // Xóa thẳng trên danh sách hiển thị
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gỡ bài và hủy phiên đấu giá thành công!");
                            } else {
                                String errorMsg = (response != null) ? response.getMessage() : "Server từ chối.";
                                showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể gỡ sản phẩm: " + errorMsg);
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Lỗi gửi lệnh: " + e.getMessage()));
                    }
                });
                deleteWorker.setDaemon(true);
                deleteWorker.start();
            }
        });
    }

    private void loadItemDatabase() {
        masterData.clear();
        Task<List<Map<String, String>>> loadItemsTask = new Task<>() {
            @Override
            @SuppressWarnings("unchecked")
            protected List<Map<String, String>> call() throws Exception {
                //  ĐÃ SỬA: Khớp hoàn toàn với file Enum MessageType của bạn
                Request request = new Request(MessageType.GET_PRODUCTS, null);

                Response response = ClientSocket.getInstance().sendRequest(request);
                if (response != null && response.isSuccess()) {
                    return (List<Map<String, String>>) response.getData();
                }
                return null;
            }
        };

        loadItemsTask.setOnSucceeded(event -> {
            List<Map<String, String>> items = loadItemsTask.getValue();
            if (items != null) {
                for (Map<String, String> itemMap : items) {
                    masterData.add(new AdminItemRow(
                            itemMap.get("id"),
                            itemMap.get("title"), // hoặc "itemName" tùy thuộc key Server trả về
                            itemMap.get("sellerName"),
                            itemMap.get("currentPrice"),
                            itemMap.get("status")
                    ));
                }
            }
        });

        Thread thread = new Thread(loadItemsTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setupSearchFilter() {
        FilteredList<AdminItemRow> filteredData = new FilteredList<>(masterData, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return row.getItemName().toLowerCase().contains(lowerCaseFilter) ||
                        row.getSeller().toLowerCase().contains(lowerCaseFilter);
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

    // ── LỚP TRỢ GIÚP BIỂU DIỄN DÒNG SẢN PHẨM (ROW MODEL) ──────────────────────────────────
    public static class AdminItemRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty itemName;
        private final SimpleStringProperty seller;
        private final SimpleStringProperty currentPrice;
        private final SimpleStringProperty status;

        public AdminItemRow(String id, String itemName, String seller, String currentPrice, String status) {
            this.id = new SimpleStringProperty(id);
            this.itemName = new SimpleStringProperty(itemName);
            this.seller = new SimpleStringProperty(seller);
            this.currentPrice = new SimpleStringProperty(currentPrice);
            this.status = new SimpleStringProperty(status);
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
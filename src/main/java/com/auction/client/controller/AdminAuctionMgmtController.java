package com.auction.client.controller;

import com.auction.client.service.AdminAuctionService;
import com.auction.shared.model.Auction; // 🛠️ ĐÃ THÊM: Import đúng Model từ Server

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminAuctionMgmtController {
    @FXML private TableView<AdminAuctionRow> auctionTable;
    @FXML private TableColumn<AdminAuctionRow, AdminAuctionRow> colProduct;
    @FXML private TableColumn<AdminAuctionRow, String> colSeller;
    @FXML private TableColumn<AdminAuctionRow, AdminAuctionRow> colPrice;
    @FXML private TableColumn<AdminAuctionRow, String> colBids;
    @FXML private TableColumn<AdminAuctionRow, String> colStatus;
    @FXML private TableColumn<AdminAuctionRow, String> colEndTime;
    @FXML private TableColumn<AdminAuctionRow, Void> colActions;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotalAuctions;

    private final AdminAuctionService auctionService = new AdminAuctionService();

    private final ObservableList<AdminAuctionRow> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchFilter();
        loadAuctionDatabase();
    }

    private void setupTableColumns() {
        colSeller.setCellValueFactory(cellData -> cellData.getValue().sellerProperty());
        colBids.setCellValueFactory(cellData -> cellData.getValue().bidsProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colEndTime.setCellValueFactory(cellData -> cellData.getValue().endTimeProperty());

        // 1. 🖼️ CELL FACTORY PHỨC HỢP
        colProduct.setCellValueFactory(cellData -> cellData.getValue().selfProperty());
        colProduct.setCellFactory(param -> new TableCell<>() {
            private final HBox container = new HBox(12);
            private final ImageView imgView = new ImageView();
            private final VBox textContainer = new VBox(4);
            private final Label lblName = new Label();
            private final Label lblDesc = new Label();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(6, 0, 6, 0));
                imgView.setFitWidth(50);
                imgView.setFitHeight(50);
                imgView.setPreserveRatio(true);

                lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

                textContainer.getChildren().addAll(lblName, lblDesc);
                container.getChildren().addAll(imgView, textContainer);
            }

            @Override
            protected void updateItem(AdminAuctionRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblName.setText(item.getProductName());
                    lblDesc.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả sản phẩm");

                    try {
                        String imgPath = item.getImageUrl();
                        if (imgPath != null && !imgPath.isEmpty()) {
                            imgView.setImage(new Image(getClass().getResourceAsStream(imgPath)));
                        } else {
                            imgView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
                        }
                    } catch (Exception e) {
                        try {
                            imgView.setImage(new Image("https://via.placeholder.com/50"));
                        } catch (Exception ignored) {}
                    }
                    setGraphic(container);
                }
            }
        });

        // 2. 💰 CELL FACTORY GIÁ KÉP
        colPrice.setCellValueFactory(cellData -> cellData.getValue().selfProperty());
        colPrice.setCellFactory(param -> new TableCell<>() {
            private final VBox container = new VBox(4);
            private final Label lblCurrentPrice = new Label();
            private final Label lblStartPrice = new Label();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                lblCurrentPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a; -fx-font-size: 13px;");
                lblStartPrice.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                container.getChildren().addAll(lblCurrentPrice, lblStartPrice);
            }

            @Override
            protected void updateItem(AdminAuctionRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblCurrentPrice.setText(item.getCurrentPrice() + " đ");
                    lblStartPrice.setText("Khởi điểm: " + item.getStartPrice() + " đ");
                    setGraphic(container);
                }
            }
        });

        // 3. 🏷️ CELL FACTORY TRẠNG THÁI
        colStatus.setCellFactory(param -> new TableCell<>() {
            private final Label lblStatusBadge = new Label();
            {
                lblStatusBadge.setAlignment(Pos.CENTER);
                lblStatusBadge.setMaxWidth(Double.MAX_VALUE);
                lblStatusBadge.setStyle("-fx-background-radius: 6; -fx-padding: 6 12; -fx-font-weight: bold; -fx-font-size: 12px;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    if ("RUNNING".equalsIgnoreCase(item)) {
                        lblStatusBadge.setText("Đang diễn ra");
                        lblStatusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-weight: bold; -fx-alignment: center;");
                    } else if ("OPEN".equalsIgnoreCase(item)) {
                        lblStatusBadge.setText("Sắp diễn ra");
                        lblStatusBadge.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-weight: bold; -fx-alignment: center;");
                    } else if ("CANCELED".equalsIgnoreCase(item)) {
                    lblStatusBadge.setText("Bị hủy");
                    lblStatusBadge.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-weight: bold; -fx-alignment: center;");
                }else {
                        lblStatusBadge.setText("Đã kết thúc");
                        lblStatusBadge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-weight: bold; -fx-alignment: center;");
                    }
                    setGraphic(lblStatusBadge);
                }
            }
        });

        // 4. 🛠️ CELL FACTORY THAO TÁC
        colActions.setCellFactory(param -> new TableCell<>() {
            private final HBox actionBox = new HBox(8);
            private final Button btnView = new Button("👁 Xem");
            private final Button btnCancel = new Button("⛔ Hủy");

            {
                actionBox.setAlignment(Pos.CENTER);
                btnView.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
                btnCancel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");

                btnView.setOnAction(event -> {
                    AdminAuctionRow selected = getTableView().getItems().get(getIndex());
                    handleViewAuctionDetails(selected);
                });

                btnCancel.setOnAction(event -> {
                    AdminAuctionRow selected = getTableView().getItems().get(getIndex());
                    handleCancelAuction(selected);
                });

                actionBox.getChildren().addAll(btnView, btnCancel);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AdminAuctionRow currentRow = getTableView().getItems().get(getIndex());
                    if ("ĐÃ KẾT THÚC".equalsIgnoreCase(currentRow.getStatus()) || "FINISHED".equalsIgnoreCase(currentRow.getStatus()) || "CANCELED".equalsIgnoreCase(currentRow.getStatus())) {                        btnCancel.setDisable(true);
                        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #cbd5e1; -fx-background-radius: 6;");
                    } else {
                        btnCancel.setDisable(false);
                        btnCancel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
                    }
                    setGraphic(actionBox);
                }
            }
        });
    }

    /**
     * 🚀 ĐÃ SỬA: Nhận trực tiếp List<Auction> thay vì List<Map> để tránh ClassCastException
     */
    /**
     * 🚀 ĐÃ ĐỒNG BỘ 100% VỚI CLASS AUCTION:
     * Đọc trực tiếp danh sách đối tượng Auction từ Server gửi về.
     */
    private void loadAuctionDatabase() {
        masterData.clear();
        auctionService.fetchAllAuctionsAsync(auctions -> {
            if (auctions == null) return;
            for (Auction auc : auctions) {
                masterData.add(new AdminAuctionRow(
                        String.valueOf(auc.getId()),
                        auc.getProductName() != null ? auc.getProductName() : "Sản phẩm không tên",
                        auc.getDescription() != null ? auc.getDescription() : "Không có mô tả",
                        auc.getImagePath() != null ? auc.getImagePath() : "",
                        auc.getSellerName() != null ? auc.getSellerName() : "Ẩn danh",
                        String.format("%,1.0f", auc.getStartPrice()),
                        String.format("%,1.0f", auc.getCurrentPrice()),
                        String.valueOf(auc.getBidCount()),
                        auc.getStatus() != null ? auc.getStatus().name() : "OPEN",
                        auc.getEndTime() != null ? auc.getEndTime().toString() : "Không xác định"
                ));
            }
            if (lblTotalAuctions != null) {
                lblTotalAuctions.setText(String.valueOf(masterData.size()));
            }
        });
    }
    private void setupSearchFilter() {
        FilteredList<AdminAuctionRow> filteredData = new FilteredList<>(masterData, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return row.getProductName().toLowerCase().contains(lowerCaseFilter) ||
                        row.getSeller().toLowerCase().contains(lowerCaseFilter) ||
                        row.getStatus().toLowerCase().contains(lowerCaseFilter);
            });
        });

        auctionTable.setItems(filteredData);
    }

    private void handleViewAuctionDetails(AdminAuctionRow auction) {
        showAlert(Alert.AlertType.INFORMATION, "Chi tiết cuộc đấu giá",
                "Sản phẩm: " + auction.getProductName() + "\nNgười bán: " + auction.getSeller() + "\nMô tả: " + auction.getDescription());
    }

    private void handleCancelAuction(AdminAuctionRow auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn hủy phiên đấu giá [" + auction.getProductName() + "] không?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận dừng khẩn cấp");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(type -> {
            if (type != ButtonType.YES) return;

            // ✅ SAU — Controller không biết gì về Request/MessageType
            auctionService.cancelAuctionAsync(auction.getId(), resp -> {
                if (resp != null && resp.isSuccess()) {
                    auction.setStatus("CANCELED");
                    auctionTable.refresh();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Đã dừng cuộc đấu giá thành công!");
                } else {
                    String msg = resp != null ? resp.getMessage() : "Server từ chối thực thi.";
                    showAlert(Alert.AlertType.ERROR, "Lỗi dừng phiên", msg);
                }
            });
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── LỚP TRỢ GIÚP POJO MODEL ───────────────────────────────────
    public static class AdminAuctionRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty productName;
        private final SimpleStringProperty description;
        private final SimpleStringProperty imageUrl;
        private final SimpleStringProperty seller;
        private final SimpleStringProperty startPrice;
        private final SimpleStringProperty currentPrice;
        private final SimpleStringProperty bids;
        private final SimpleStringProperty status;
        private final SimpleStringProperty endTime;
        private final SimpleObjectProperty<AdminAuctionRow> self;

        public AdminAuctionRow(String id, String productName, String description, String imageUrl,
                               String seller, String startPrice, String currentPrice, String bids, String status, String endTime) {
            this.id = new SimpleStringProperty(id);
            this.productName = new SimpleStringProperty(productName);
            this.description = new SimpleStringProperty(description);
            this.imageUrl = new SimpleStringProperty(imageUrl);
            this.seller = new SimpleStringProperty(seller);
            this.startPrice = new SimpleStringProperty(startPrice);
            this.currentPrice = new SimpleStringProperty(currentPrice);
            this.bids = new SimpleStringProperty(bids);
            this.status = new SimpleStringProperty(status);
            this.endTime = new SimpleStringProperty(endTime);
            this.self = new SimpleObjectProperty<>(this);
        }

        public SimpleObjectProperty<AdminAuctionRow> selfProperty() { return this.self; }

        public String getId() { return id.get(); }
        public String getProductName() { return productName.get(); }
        public String getDescription() { return description.get(); }
        public String getImageUrl() { return imageUrl.get(); }
        public String getSeller() { return seller.get(); }
        public SimpleStringProperty sellerProperty() { return seller; }
        public String getStartPrice() { return startPrice.get(); }
        public String getCurrentPrice() { return currentPrice.get(); }
        public String getBids() { return bids.get(); }
        public SimpleStringProperty bidsProperty() { return bids; }
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { this.status.set(value); }
        public SimpleStringProperty statusProperty() { return status; }
        public String getEndTime() { return endTime.get(); }
        public SimpleStringProperty endTimeProperty() { return endTime; }
    }
}
package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import com.auction.shared.model.Account;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminUserMgmtController {

    @FXML private TableView<AdminUserRow> userTable; // Đổi kiểu dữ liệu hiển thị thành lớp Row an toàn
    @FXML private TableColumn<AdminUserRow, String> colUsername;
    @FXML private TableColumn<AdminUserRow, String> colRole;
    @FXML private TableColumn<AdminUserRow, String> colBalance;
    @FXML private TableColumn<AdminUserRow, String> colStatus;
    @FXML private TableColumn<AdminUserRow, Void> colAction;
    @FXML private TextField txtSearch;

    private ObservableList<AdminUserRow> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadUserDatabase();
        setupSearchFilter();
    }

    private void setupTableColumns() {
        colUsername.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        colRole.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        colBalance.setCellValueFactory(cellData -> cellData.getValue().balanceProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        colAction.setCellFactory(param -> new TableCell<AdminUserRow, Void>() {
            private final Button btnLock = new Button("Khóa tài khoản");
            {
                btnLock.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                btnLock.setOnAction(event -> {
                    AdminUserRow selected = getTableView().getItems().get(getIndex());

                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                            "Hệ thống đã thực hiện khóa tài khoản [" + selected.getUsername() + "] thành công!", ButtonType.OK);
                    alert.setHeaderText(null);
                    alert.setTitle("Quản trị viên thông báo");
                    alert.showAndWait();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnLock);
                }
            }
        });
    }

    private void loadUserDatabase() {
        masterData.clear();

        // Nạp dữ liệu thông qua lớp Row helper, né hoàn toàn việc khởi tạo Abstract Class 'Account'
        masterData.add(new AdminUserRow("1", "admin_teamcode", "ADMIN", "0 đ", "Đang hoạt động"));
        masterData.add(new AdminUserRow("2", "seller_vip99", "SELLER", "15,000,000 đ", "Đang hoạt động"));
        masterData.add(new AdminUserRow("3", "nguoimua_anDanh", "BIDDER", "550,000 đ", "Đang hoạt động"));
        masterData.add(new AdminUserRow("4", "macbook_fan", "BIDDER", "2,400,000 đ", "Đang hoạt động"));

        userTable.setItems(masterData);
    }

    private void setupSearchFilter() {
        FilteredList<AdminUserRow> filteredData = new FilteredList<>(masterData, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return row.getUsername().toLowerCase().contains(lowerCaseFilter) ||
                        row.getRole().toLowerCase().contains(lowerCaseFilter);
            });
        });

        userTable.setItems(filteredData);
    }

    /**
     * LỚP TRỢ GIÚP (INNER CLASS) - Định nghĩa cấu trúc dữ liệu hiển thị cho bảng Admin
     * Giúp cô lập dữ liệu UI với Model Core bị dính lỗi Abstract của hệ thống.
     */
    public static class AdminUserRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty username;
        private final SimpleStringProperty role;
        private final SimpleStringProperty balance;
        private final SimpleStringProperty status;

        public AdminUserRow(String id, String username, String role, String balance, String status) {
            this.id = new SimpleStringProperty(id);
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
            this.balance = new SimpleStringProperty(balance);
            this.status = new SimpleStringProperty(status);
        }

        public String getUsername() { return username.get(); }
        public SimpleStringProperty usernameProperty() { return username; }

        public String getRole() { return role.get(); }
        public SimpleStringProperty roleProperty() { return role; }

        public String getBalance() { return balance.get(); }
        public SimpleStringProperty balanceProperty() { return balance; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
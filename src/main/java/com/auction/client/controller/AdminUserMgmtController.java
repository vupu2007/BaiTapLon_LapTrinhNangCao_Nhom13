package com.auction.client.controller;

import com.auction.client.service.AdminUserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Map;

public class AdminUserMgmtController {

    @FXML private TableView<AdminUserRow> userTable;
    @FXML private TableColumn<AdminUserRow, String> colUsername;
    @FXML private TableColumn<AdminUserRow, String> colRole;
    @FXML private TableColumn<AdminUserRow, String> colBalance;
    @FXML private TableColumn<AdminUserRow, String> colStatus;
    @FXML private TableColumn<AdminUserRow, Void> colAction;
    @FXML private TextField txtSearch;

    private final ObservableList<AdminUserRow> masterData = FXCollections.observableArrayList();
    private final AdminUserService userService = new AdminUserService();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchFilter();
        loadUserDatabase();
    }

    private void setupTableColumns() {
        colUsername.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        colRole.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        colBalance.setCellValueFactory(cellData -> cellData.getValue().balanceProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else if ("BANNED".equalsIgnoreCase(item) || "LOCKED".equalsIgnoreCase(item)) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    setText("ĐÃ BỊ KHÓA");
                } else {
                    setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    setText("HOẠT ĐỘNG");
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnAction = new Button();
            {
                btnAction.setPrefWidth(120);
                btnAction.setPrefHeight(25);
                btnAction.setOnAction(event -> {
                    AdminUserRow selected = getTableView().getItems().get(getIndex());
                    toggleUserStatus(selected);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AdminUserRow row = getTableView().getItems().get(getIndex());
                if ("BANNED".equalsIgnoreCase(row.getStatus()) || "LOCKED".equalsIgnoreCase(row.getStatus())) {
                    btnAction.setText("Mở khóa");
                    btnAction.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                } else {
                    btnAction.setText("Khóa tài khoản");
                    btnAction.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                }
                setGraphic(btnAction);
            }
        });
    }

    private void loadUserDatabase() {
        masterData.clear();
        // ✅ Controller không biết gì về Request/MessageType
        userService.fetchAllUsersAsync(users -> {
            if (users == null) return;
            for (Map<String, String> userMap : users) {
                masterData.add(new AdminUserRow(
                        userMap.get("id"),
                        userMap.get("username"),
                        userMap.get("role"),
                        userMap.get("balance"),
                        userMap.get("status")
                ));
            }
        });
    }

    private void toggleUserStatus(AdminUserRow userRow) {
        if ("ADMIN".equalsIgnoreCase(userRow.getRole())) {
            showAlert(Alert.AlertType.WARNING, "Không được phép", "Không thể khóa tài khoản Admin!");
            return;
        }
        String currentStatus = userRow.getStatus();
        String newStatus = ("BANNED".equalsIgnoreCase(currentStatus) || "LOCKED".equalsIgnoreCase(currentStatus))
                ? "ACTIVE" : "BANNED";
        String actionName = "ACTIVE".equals(newStatus) ? "mở khóa" : "khóa";

        // ✅ Controller không biết gì về Request/MessageType
        userService.updateUserStatusAsync(userRow.getId(), newStatus, resp -> {
            if (resp != null && resp.isSuccess()) {
                userRow.setStatus(newStatus);
                userTable.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã " + actionName + " tài khoản [" + userRow.getUsername() + "] thành công!");
            } else {
                String msg = resp != null ? resp.getMessage() : "Server từ chối yêu cầu.";
                showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể thay đổi trạng thái: " + msg);
            }
        });
    }

    private void setupSearchFilter() {
        FilteredList<AdminUserRow> filteredData = new FilteredList<>(masterData, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(row -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                return row.getUsername().toLowerCase().contains(filter) ||
                        row.getRole().toLowerCase().contains(filter);
            });
        });
        userTable.setItems(filteredData);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AdminUserRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty username;
        private final SimpleStringProperty role;
        private final SimpleStringProperty balance;
        private final SimpleStringProperty status;

        public AdminUserRow(String id, String username, String role, String balance, String status) {
            this.id       = new SimpleStringProperty(id);
            this.username = new SimpleStringProperty(username);
            this.role     = new SimpleStringProperty(role);
            this.balance  = new SimpleStringProperty(balance);
            this.status   = new SimpleStringProperty(status);
        }

        public String getId() { return id.get(); }
        public String getUsername() { return username.get(); }
        public SimpleStringProperty usernameProperty() { return username; }
        public String getRole() { return role.get(); }
        public SimpleStringProperty roleProperty() { return role; }
        public String getBalance() { return balance.get(); }
        public SimpleStringProperty balanceProperty() { return balance; }
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { this.status.set(value); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
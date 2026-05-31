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

public class AdminUserMgmtController {

    @FXML private TableView<AdminUserRow> userTable;
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
        setupSearchFilter();
        loadUserDatabase();
    }

    private void setupTableColumns() {
        colUsername.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        colRole.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        colBalance.setCellValueFactory(cellData -> cellData.getValue().balanceProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // 🌟 NÂNG CẤP: Tô màu cho Cột Trạng Thái (Status Column)
        colStatus.setCellFactory(column -> new TableCell<AdminUserRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("BANNED".equalsIgnoreCase(item) || "LOCKED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Đỏ: Khóa
                        setText("ĐÃ BỊ KHÓA");
                    } else {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;"); // Xanh: Hoạt động
                        setText("HOẠT ĐỘNG");
                    }
                }
            }
        });

        // 🌟 CHUẨN HÓA: Nút Action tự động đổi màu và chức năng Khóa / Mở khóa
        colAction.setCellFactory(param -> new TableCell<AdminUserRow, Void>() {
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
                if (empty) {
                    setGraphic(null);
                } else {
                    AdminUserRow currentRow = getTableView().getItems().get(getIndex());
                    if ("BANNED".equalsIgnoreCase(currentRow.getStatus()) || "LOCKED".equalsIgnoreCase(currentRow.getStatus())) {
                        btnAction.setText("Mở khóa");
                        btnAction.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                    } else {
                        btnAction.setText("Khóa tài khoản");
                        btnAction.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                    }
                    setGraphic(btnAction);
                }
            }
        });
    }

    private void toggleUserStatus(AdminUserRow userRow) {
        if ("ADMIN".equalsIgnoreCase(userRow.getRole())) {
            showAlert(Alert.AlertType.WARNING, "Không được phép", "Không thể khóa tài khoản Admin!");
            return;
        }
        String currentStatus = userRow.getStatus();
        String newStatus = ("BANNED".equalsIgnoreCase(currentStatus) || "LOCKED".equalsIgnoreCase(currentStatus)) ? "ACTIVE" : "BANNED";
        String actionName = "ACTIVE".equals(newStatus) ? "mở khóa" : "khóa";

        String[] updateData = { userRow.getId(), newStatus };
        Request request = new Request(MessageType.UPDATE_USER_STATUS, updateData);

        Thread statusWorker = new Thread(() -> {
            try {
                Response response = ClientSocket.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        userRow.setStatus(newStatus);
                        userTable.refresh();
                        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Hệ thống đã thực hiện " + actionName + " tài khoản [" + userRow.getUsername() + "] thành công!");
                    } else {
                        String msg = (response != null) ? response.getMessage() : "Server từ chối yêu cầu.";
                        showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể thay đổi trạng thái: " + msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Lỗi gửi lệnh lên hệ thống: " + e.getMessage());
                });
            }
        });
        statusWorker.setDaemon(true);
        statusWorker.start();
    }

    private void loadUserDatabase() {
        masterData.clear();

        Task<List<Map<String, String>>> loadDataTask = new Task<>() {
            @Override
            @SuppressWarnings("unchecked")
            protected List<Map<String, String>> call() throws Exception {
                Request request = new Request(MessageType.GET_ALL_USERS, null);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response != null && response.isSuccess()) {
                    return (List<Map<String, String>>) response.getData();
                }
                return null;
            }
        };

        loadDataTask.setOnSucceeded(event -> {
            List<Map<String, String>> users = loadDataTask.getValue();
            if (users != null) {
                for (Map<String, String> userMap : users) {
                    masterData.add(new AdminUserRow(
                            userMap.get("id"),
                            userMap.get("username"),
                            userMap.get("role"),
                            userMap.get("balance"),
                            userMap.get("status")
                    ));
                }
            }
        });

        loadDataTask.setOnFailed(event -> {
            System.err.println("❌ Lỗi lấy danh sách thành viên từ Server: " + loadDataTask.getException().getMessage());
        });

        Thread thread = new Thread(loadDataTask);
        thread.setDaemon(true);
        thread.start();
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
            this.id = new SimpleStringProperty(id);
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
            this.balance = new SimpleStringProperty(balance);
            this.status = new SimpleStringProperty(status);
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
package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTotalDeposit;
    @FXML private Label lblTotalWithdraw;
    @FXML private TextField txtDeposit;
    @FXML private TextField txtWithdraw;
    @FXML private VBox transactionContainer;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @FXML
    public void initialize() {
        if (transactionContainer != null) transactionContainer.getChildren().clear();
        updateWalletUI();
        // Chạy ngầm việc tải lịch sử từ DB khi vừa vào màn hình, tránh đơ ví
        loadTransactionHistory();
    }

    private void updateWalletUI() {
        double balance = CurrentAccount.getBalance();
        double totalDeposit = CurrentAccount.getTotalDeposit();
        double totalWithdraw = CurrentAccount.getTotalWithdraw();

        System.out.println("LOG UI: Số dư=" + balance + " | Tổng nạp=" + totalDeposit + " | Tổng chi=" + totalWithdraw);

        lblBalance.setText(formatter.format(balance) + " đ");
        lblTotalDeposit.setText(formatter.format(totalDeposit) + " đ");
        lblTotalWithdraw.setText(formatter.format(totalWithdraw) + " đ");
    }

    private void loadTransactionHistory() {
        if (transactionContainer == null) return;
        transactionContainer.getChildren().clear();
        int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // 🚀 Chạy ngầm tải lịch sử giao dịch
        Thread loaderWorker = new Thread(() -> {
            try {
                Request request = new Request(MessageType.GET_TRANSACTIONS, accountId);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response != null && response.isSuccess()) {
                    // 🌟 DÙNG ĐÚNG .getData() và kiểm tra kiểu dữ liệu an toàn trước khi ép kiểu
                    Object rawData = response.getData();
                    if (rawData instanceof List) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) rawData;

                        // Vẽ giao diện động đẩy về luồng UI chính
                        Platform.runLater(() -> {
                            for (Map<String, Object> tx : list) {
                                try {
                                    boolean isDeposit = "DEPOSIT".equalsIgnoreCase(String.valueOf(tx.get("type")));

                                    // Bẫy ép kiểu ngày tháng an toàn (xử lý cả Timestamp lẫn LocalDateTime)
                                    Object timeObj = tx.get("created_at");
                                    LocalDateTime createdAt = LocalDateTime.now();
                                    if (timeObj instanceof LocalDateTime) {
                                        createdAt = (LocalDateTime) timeObj;
                                    } else if (timeObj instanceof java.sql.Timestamp) {
                                        createdAt = ((java.sql.Timestamp) timeObj).toLocalDateTime();
                                    } else if (timeObj instanceof java.util.Date) {
                                        createdAt = ((java.util.Date) timeObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                    }

                                    addTransactionToHistory(
                                            isDeposit ? "Nạp tiền thành công" : "Rút tiền thành công",
                                            tx.get("description") != null ? (String) tx.get("description") : "Không có mô tả",
                                            tx.get("amount") != null ? Double.parseDouble(tx.get("amount").toString()) : 0.0,
                                            isDeposit,
                                            createdAt
                                    );
                                } catch (Exception ex) {
                                    System.err.println("Bỏ qua 1 giao dịch lỗi định dạng: " + ex.getMessage());
                                }
                            }
                        });
                    } else {
                        // 🌟 Nếu Server trả về HashMap lỗi, nó sẽ rơi vào đây chứ không làm sập App nữa!
                        System.err.println("⚠️ CẢNH BÁO: Server phản hồi success nhưng data không phải là List! Thực tế là: " + (rawData != null ? rawData.getClass().getName() : "null"));
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi load lịch sử giao dịch chạy ngầm: " + e.getMessage());
            }
        }, "WalletHistoryLoaderThread");
        loaderWorker.setDaemon(true);
        loaderWorker.start();
    }

    @FXML
    private void handleDeposit() {
        String amountStr = txtDeposit.getText().trim();
        if (amountStr.isEmpty()) { showNotify("Thông báo", "Vui lòng nhập số tiền cần nạp!"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { showNotify("Lỗi nhập liệu", "Số tiền nạp vào phải lớn hơn 0 đ!"); return; }

            int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
            Object[] data = {accountId, amount, "DEPOSIT"};
            Request request = new Request(MessageType.WALLET_TRANSACTION, data);

            // 🚀 Chạy luồng xử lý nạp tiền ngầm
            Thread depositWorker = new Thread(() -> {
                try {
                    Response response = ClientSocket.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            CurrentAccount.deposit(amount);
                            addTransactionToHistory("Nạp tiền thành công", "Chuyển khoản", amount, true, null);
                            updateWalletUI();
                            txtDeposit.clear();
                            showNotify("Thành công", "Đã nạp thành công " + formatter.format(amount) + " đ vào ví!");
                        } else {
                            showNotify("Thất bại", "Không thể thực hiện giao dịch!");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showNotify("Lỗi mạng", "Không thể kết nối đến máy chủ!"));
                }
            }, "WalletDepositThread");
            depositWorker.setDaemon(true);
            depositWorker.start();

        } catch (NumberFormatException e) {
            showNotify("Sai định dạng", "Vui lòng chỉ gõ số nguyên, không nhập chữ.");
        }
    }

    @FXML
    private void handleWithdraw() {
        String amountStr = txtWithdraw.getText().trim();
        if (amountStr.isEmpty()) { showNotify("Thông báo", "Vui lòng nhập số tiền cần rút!"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { showNotify("Lỗi nhập liệu", "Số tiền rút ra phải lớn hơn 0 đ!"); return; }

            // Kiểm tra số dư khả dụng trước khi gọi mạng
            if (CurrentAccount.getBalance() < amount) {
                showNotify("Rút tiền thất bại", "Số dư khả dụng trong ví không đủ!");
                return;
            }

            int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
            Object[] data = {accountId, amount, "WITHDRAW"};
            Request request = new Request(MessageType.WALLET_TRANSACTION, data);

            // 🚀 Chạy luồng xử lý rút tiền ngầm
            Thread withdrawWorker = new Thread(() -> {
                try {
                    Response response = ClientSocket.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            CurrentAccount.withdraw(amount);
                            addTransactionToHistory("Rút tiền thành công", "Ví điện tử / Ngân hàng", amount, false, null);
                            updateWalletUI();
                            txtWithdraw.clear();
                            showNotify("Thành công", "Đã rút thành công " + formatter.format(amount) + " đ khỏi ví!");
                        } else {
                            showNotify("Thất bại", "Không thể thực hiện giao dịch!");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showNotify("Lỗi mạng", "Không thể kết nối đến máy chủ!"));
                }
            }, "WalletWithdrawThread");
            withdrawWorker.setDaemon(true);
            withdrawWorker.start();

        } catch (NumberFormatException e) {
            showNotify("Sai định dạng", "Vui lòng chỉ gõ số nguyên, không nhập chữ.");
        }
    }

    private void addTransactionToHistory(String title, String type, double amount, boolean isDeposit, LocalDateTime createdAt) {
        if (transactionContainer == null) return;

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0; -fx-padding: 15 0;");

        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(20);
        circle.setStroke(javafx.scene.paint.Color.TRANSPARENT);
        circle.setFill(javafx.scene.paint.Color.web(isDeposit ? "#ecfdf5" : "#fef2f2"));

        VBox textContainer = new VBox();
        textContainer.setSpacing(3);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String time = (createdAt != null ? createdAt : LocalDateTime.now())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblSub = new Label(type + " • " + time);
        lblSub.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        textContainer.getChildren().addAll(lblTitle, lblSub);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label lblAmount = new Label((isDeposit ? "+" : "-") + formatter.format(amount) + " đ");
        lblAmount.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: " + (isDeposit ? "#059669" : "#dc2626") + ";");

        row.getChildren().addAll(circle, textContainer, spacer, lblAmount);

        if (createdAt == null) {
            transactionContainer.getChildren().add(0, row);
        } else {
            transactionContainer.getChildren().add(row);
        }
    }

    private void showNotify(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}
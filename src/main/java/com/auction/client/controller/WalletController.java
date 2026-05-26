package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTotalDeposit;
    @FXML private Label lblTotalWithdraw;
    @FXML private TextField txtDeposit;
    @FXML private TextField txtWithdraw;
    @FXML private VBox transactionContainer;

    // Nút bấm cần quản lý trạng thái chống spam click
    @FXML private Button btnDeposit;
    @FXML private Button btnWithdraw;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    // 🌟 TỐI ƯU CỐT LÕI: Executor duy nhất xử lý tuần tự mọi giao dịch tài chính, chống Race Condition tuyệt đối
    private static final ExecutorService walletExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WalletTransactionExecutor");
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize() {
        if (transactionContainer != null) transactionContainer.getChildren().clear();
        updateWalletUI();
        loadTransactionHistory();
    }

    private void updateWalletUI() {
        double balance = CurrentAccount.getBalance();
        double totalDeposit = CurrentAccount.getTotalDeposit();
        double totalWithdraw = CurrentAccount.getTotalWithdraw();

        if (lblBalance != null) lblBalance.setText(formatter.format(balance) + " đ");
        if (lblTotalDeposit != null) lblTotalDeposit.setText(formatter.format(totalDeposit) + " đ");
        if (lblTotalWithdraw != null) lblTotalWithdraw.setText(formatter.format(totalWithdraw) + " đ");
    }

    private void loadTransactionHistory() {
        if (transactionContainer == null || CurrentAccount.getAccount() == null) return;

        int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // Đẩy tác vụ vào Thread Pool tuần tự
        walletExecutor.submit(() -> {
            try {
                Request request = new Request(MessageType.GET_TRANSACTIONS, accountId);
                Response response = ClientSocket.getInstance().sendRequest(request);

                if (response != null && response.isSuccess()) {
                    Object rawData = response.getData();
                    if (rawData instanceof List) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) rawData;

                        // 🌟 GIẢI PHÁP LUỒNG: Xử lý build danh sách Node đồ họa trên luồng ngầm trước
                        List<HBox> compiledRows = new ArrayList<>();
                        for (Map<String, Object> tx : list) {
                            try {
                                boolean isDeposit = "DEPOSIT".equalsIgnoreCase(String.valueOf(tx.get("type")));
                                Object timeObj = tx.get("created_at");
                                LocalDateTime createdAt = LocalDateTime.now();
                                if (timeObj instanceof LocalDateTime) {
                                    createdAt = (LocalDateTime) timeObj;
                                } else if (timeObj instanceof java.sql.Timestamp) {
                                    createdAt = ((java.sql.Timestamp) timeObj).toLocalDateTime();
                                } else if (timeObj instanceof java.util.Date) {
                                    createdAt = ((java.util.Date) timeObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                }

                                HBox row = buildTransactionRowNode(
                                        isDeposit ? "Nạp tiền thành công" : "Rút tiền thành công",
                                        tx.get("description") != null ? (String) tx.get("description") : "Không có mô tả",
                                        tx.get("amount") != null ? Double.parseDouble(tx.get("amount").toString()) : 0.0,
                                        isDeposit,
                                        createdAt
                                );
                                compiledRows.add(row);
                            } catch (Exception ex) {
                                System.err.println("Bỏ qua 1 giao dịch lỗi định dạng: " + ex.getMessage());
                            }
                        }

                        // 🌟 Đẩy cục bộ giao diện đã dựng sẵn về UI Thread trong đúng 1 khung hình duy nhất
                        Platform.runLater(() -> {
                            transactionContainer.getChildren().clear();
                            transactionContainer.getChildren().addAll(compiledRows);
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi load lịch sử giao dịch: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeposit() {
        if (txtDeposit == null) return;
        String amountStr = txtDeposit.getText().trim();
        if (amountStr.isEmpty()) { showNotify("Thông báo", "Vui lòng nhập số tiền cần nạp!"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { showNotify("Lỗi nhập liệu", "Số tiền nạp vào phải lớn hơn 0 đ!"); return; }

            // 🌟 CHỐNG SPAM: Vô hiệu hóa nút bấm ngay lập tức trên UI
            setButtonsLoading(true);

            int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
            Object[] data = {accountId, amount, "DEPOSIT"};
            Request request = new Request(MessageType.WALLET_TRANSACTION, data);

            walletExecutor.submit(() -> {
                try {
                    Response response = ClientSocket.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        setButtonsLoading(false);
                        if (response != null && response.isSuccess()) {
                            CurrentAccount.deposit(amount);

                            // Tạo hàng động chèn lên đầu danh sách mượt mà
                            HBox newRow = buildTransactionRowNode("Nạp tiền thành công", "Chuyển khoản", amount, true, LocalDateTime.now());
                            if (transactionContainer != null) transactionContainer.getChildren().add(0, newRow);

                            updateWalletUI();
                            txtDeposit.clear();
                            showNotify("Thành công", "Đã nạp thành công " + formatter.format(amount) + " đ vào ví!");
                        } else {
                            showNotify("Thất bại", "Không thể thực hiện giao dịch nạp tiền!");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setButtonsLoading(false);
                        showNotify("Lỗi mạng", "Không thể kết nối đến máy chủ!");
                    });
                }
            });

        } catch (NumberFormatException e) {
            showNotify("Sai định dạng", "Vui lòng chỉ gõ số nguyên, không nhập chữ.");
        }
    }

    @FXML
    private void handleWithdraw() {
        if (txtWithdraw == null) return;
        String amountStr = txtWithdraw.getText().trim();
        if (amountStr.isEmpty()) { showNotify("Thông báo", "Vui lòng nhập số tiền cần rút!"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { showNotify("Lỗi nhập liệu", "Số tiền rút ra phải lớn hơn 0 đ!"); return; }

            if (CurrentAccount.getBalance() < amount) {
                showNotify("Rút tiền thất bại", "Số dư khả dụng trong ví không đủ!");
                return;
            }

            // 🌟 CHỐNG SPAM: Vô hiệu hóa nút bấm
            setButtonsLoading(true);

            int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
            Object[] data = {accountId, amount, "WITHDRAW"};
            Request request = new Request(MessageType.WALLET_TRANSACTION, data);

            walletExecutor.submit(() -> {
                try {
                    Response response = ClientSocket.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        setButtonsLoading(false);
                        if (response != null && response.isSuccess()) {
                            CurrentAccount.withdraw(amount);

                            HBox newRow = buildTransactionRowNode("Rút tiền thành công", "Ví điện tử / Ngân hàng", amount, false, LocalDateTime.now());
                            if (transactionContainer != null) transactionContainer.getChildren().add(0, newRow);

                            updateWalletUI();
                            txtWithdraw.clear();
                            showNotify("Thành công", "Đã rút thành công " + formatter.format(amount) + " đ khỏi ví!");
                        } else {
                            showNotify("Thất bại", "Không thể thực hiện giao dịch rút tiền!");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setButtonsLoading(false);
                        showNotify("Lỗi mạng", "Không thể kết nối đến máy chủ!");
                    });
                }
            });

        } catch (NumberFormatException e) {
            showNotify("Sai định dạng", "Vui lòng chỉ gõ số nguyên, không nhập chữ.");
        }
    }

    /**
     * 🚀 TỐI ƯU KIẾN TRÚC: Hàm này dựng sẵn Node trên luồng ngầm, giảm tải hoàn toàn cho Main Thread
     */
    private HBox buildTransactionRowNode(String title, String type, double amount, boolean isDeposit, LocalDateTime createdAt) {
        HBox row = new HBox();
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
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label lblAmount = new Label((isDeposit ? "+" : "-") + formatter.format(amount) + " đ");
        lblAmount.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: " + (isDeposit ? "#059669" : "#dc2626") + ";");

        row.getChildren().addAll(circle, textContainer, spacer, lblAmount);
        return row;
    }

    private void setButtonsLoading(boolean loading) {
        if (btnDeposit != null) btnDeposit.setDisable(loading);
        if (btnWithdraw != null) btnWithdraw.setDisable(loading);
    }

    private void showNotify(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
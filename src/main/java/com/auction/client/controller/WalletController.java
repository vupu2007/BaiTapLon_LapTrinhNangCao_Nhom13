package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.server.dao.AccountDAO;
import com.auction.server.dao.TransactionDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTotalDeposit;
    @FXML private Label lblTotalWithdraw;
    @FXML private TextField txtDeposit;
    @FXML private TextField txtWithdraw;
    @FXML private VBox transactionContainer; // Quản lý khu vực hiển thị lịch sử

    private final DecimalFormat formatter = new DecimalFormat("#,###");
    // DAO để thao tác với DB
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @FXML
    public void initialize() {
        // Xóa các dòng mẫu hardcode thiết kế trong FXML trước khi nạp giao dịch thật
        if (transactionContainer != null) transactionContainer.getChildren().clear();
        updateWalletUI();
        // Load lịch sử giao dịch từ DB khi vào màn hình
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

    // Load lịch sử từ DB — tránh mất dữ liệu khi thoát ra vào lại
    private void loadTransactionHistory() {
        if (transactionContainer == null) return;
        transactionContainer.getChildren().clear();
        int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
        List<Map<String, Object>> list = transactionDAO.getByAccount(accountId);
        for (Map<String, Object> tx : list) {
            boolean isDeposit = tx.get("type").equals("DEPOSIT");
            addTransactionToHistory(
                    isDeposit ? "Nạp tiền thành công" : "Rút tiền thành công",
                    (String) tx.get("description"),
                    (double) tx.get("amount"),
                    isDeposit,
                    (LocalDateTime) tx.get("created_at")
            );
        }
    }

    @FXML
    private void handleDeposit() {
        String amountStr = txtDeposit.getText().trim();
        if (amountStr.isEmpty()) { showNotify("Thông báo", "Vui lòng nhập số tiền cần nạp!"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { showNotify("Lỗi nhập liệu", "Số tiền nạp vào phải lớn hơn 0 đ!"); return; }

            // 1. Cập nhật số dư trên RAM
            CurrentAccount.deposit(amount);

            // 2. Đồng bộ số dư mới xuống DB
            int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
            accountDAO.updateBalance(accountId, CurrentAccount.getBalance(), CurrentAccount.getTotalDeposit(), CurrentAccount.getTotalWithdraw());

            // 3. Lưu lịch sử giao dịch vào DB
            transactionDAO.insertTransaction(accountId, "DEPOSIT", amount, CurrentAccount.getBalance());

            // 4. Thêm giao dịch vào lịch sử UI
            addTransactionToHistory("Nạp tiền thành công", "Chuyển khoản", amount, true, null);

            updateWalletUI();
            txtDeposit.clear();
            showNotify("Thành công", "Đã nạp thành công " + formatter.format(amount) + " đ vào ví!");

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

            // 1. Kiểm tra và trừ tiền trên RAM
            if (!CurrentAccount.withdraw(amount)) { showNotify("Rút tiền thất bại", "Số dư khả dụng trong ví không đủ!"); return; }

            // 2. Đồng bộ số dư mới xuống DB
            int accountId = Integer.parseInt(CurrentAccount.getAccount().getId());
            accountDAO.updateBalance(accountId, CurrentAccount.getBalance(), CurrentAccount.getTotalDeposit(), CurrentAccount.getTotalWithdraw());

            // 3. Lưu lịch sử giao dịch vào DB
            transactionDAO.insertTransaction(accountId, "WITHDRAW", amount, CurrentAccount.getBalance());

            // 4. Thêm giao dịch vào lịch sử UI
            addTransactionToHistory("Rút tiền thành công", "Ví điện tử / Ngân hàng", amount, false, null);

            updateWalletUI();
            txtWithdraw.clear();
            showNotify("Thành công", "Đã rút thành công " + formatter.format(amount) + " đ khỏi ví!");

        } catch (NumberFormatException e) {
            showNotify("Sai định dạng", "Vui lòng chỉ gõ số nguyên, không nhập chữ.");
        }
    }

    /**
     * Hàm tự động vẽ một dòng HBox chứa lịch sử và thêm thẳng vào transactionContainer
     * createdAt = null khi thêm mới (dùng giờ hiện tại), có giá trị khi load từ DB
     */
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

        // Dùng thời gian từ DB nếu có, không thì dùng giờ hiện tại
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
        // Thêm vào cuối — thứ tự đã được sắp xếp DESC từ DB
        if (createdAt == null) {
            transactionContainer.getChildren().add(0, row);
        } else {
            transactionContainer.getChildren().add(row);
        }
    }

    private void showNotify(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
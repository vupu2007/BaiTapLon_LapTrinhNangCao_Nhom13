package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.text.DecimalFormat;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTotalDeposit;
    @FXML private Label lblTotalWithdraw;
    @FXML private TextField txtDeposit;
    @FXML private TextField txtWithdraw;
    @FXML private VBox transactionContainer; // Quản lý khu vực hiển thị lịch sử

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @FXML
    public void initialize() {
        // Xóa các dòng mẫu hardcode thiết kế trong FXML trước khi nạp giao dịch thật
        if (transactionContainer != null) {
            transactionContainer.getChildren().clear();
        }
        updateWalletUI();
    }

    private void updateWalletUI() {
        double balance = CurrentAccount.getBalance();
        double totalDeposit = CurrentAccount.getTotalDeposit();
        double totalWithdraw = CurrentAccount.getTotalWithdraw();

        System.out.println("LOG UI: Số dư=" + balance + " | Tổng nạp=" + totalDeposit + " | Tổng chi=" + totalWithdraw);

        if (balance == 0) {
            lblBalance.setText("0 đ");
        } else {
            lblBalance.setText(formatter.format(balance) + " đ");
        }

        if (totalDeposit == 0) {
            lblTotalDeposit.setText("0 đ");
        } else {
            lblTotalDeposit.setText(formatter.format(totalDeposit) + " đ");
        }

        if (totalWithdraw == 0) {
            lblTotalWithdraw.setText("0 đ");
        } else {
            lblTotalWithdraw.setText(formatter.format(totalWithdraw) + " đ");
        }
    }

    @FXML
    private void handleDeposit() {
        String amountStr = txtDeposit.getText().trim();
        if (amountStr.isEmpty()) {
            showNotify("Thông báo", "Vui lòng nhập số tiền cần nạp!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showNotify("Lỗi nhập liệu", "Số tiền nạp vào phải lớn hơn 0 đ!");
                return;
            }

            CurrentAccount.deposit(amount);

            if (CurrentAccount.getAccount() != null) {
                try {
                    int accountIdInt = Integer.parseInt(CurrentAccount.getAccount().getId());
                    double newBalance = CurrentAccount.getBalance();
                    double newTotalDeposit = CurrentAccount.getTotalDeposit();
                    double newTotalWithdraw = CurrentAccount.getTotalWithdraw();

                    CurrentAccount.getAccount().setBalance(newBalance);
                    if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Bidder) {
                        ((com.auction.shared.model.Bidder) CurrentAccount.getAccount()).setTotalDeposit(newTotalDeposit);
                    } else if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Seller) {
                        ((com.auction.shared.model.Seller) CurrentAccount.getAccount()).setTotalDeposit(newTotalDeposit);
                    }

                    com.auction.server.dao.AccountDAO accountDAO = new com.auction.server.dao.AccountDAO();
                    boolean isSaved = accountDAO.updateBalance(accountIdInt, newBalance, newTotalDeposit, newTotalWithdraw);

                    if (!isSaved) {
                        System.out.println("CẢNH BÁO: Không thể cập nhật số dư mới vào cơ sở dữ liệu!");
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("LỖI: ID tài khoản không hợp lệ.");
                }
            }

            // 🔥 THÊM GIAO DỊCH VÀO LỊCH SỬ UI
            addTransactionToHistory("Nạp tiền thành công", "Chuyển khoản", amount, true);

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
        if (amountStr.isEmpty()) {
            showNotify("Thông báo", "Vui lòng nhập số tiền cần rút!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showNotify("Lỗi nhập liệu", "Số tiền rút ra phải lớn hơn 0 đ!");
                return;
            }

            boolean isSuccess = CurrentAccount.withdraw(amount);

            if (isSuccess) {
                if (CurrentAccount.getAccount() != null) {
                    try {
                        int accountIdInt = Integer.parseInt(CurrentAccount.getAccount().getId());
                        double newBalance = CurrentAccount.getBalance();
                        double newTotalDeposit = CurrentAccount.getTotalDeposit();
                        double newTotalWithdraw = CurrentAccount.getTotalWithdraw();

                        CurrentAccount.getAccount().setBalance(newBalance);
                        if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Bidder) {
                            ((com.auction.shared.model.Bidder) CurrentAccount.getAccount()).setTotalWithdraw(newTotalWithdraw);
                        } else if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Seller) {
                            ((com.auction.shared.model.Seller) CurrentAccount.getAccount()).setTotalWithdraw(newTotalWithdraw);
                        }

                        com.auction.server.dao.AccountDAO accountDAO = new com.auction.server.dao.AccountDAO();
                        boolean isSaved = accountDAO.updateBalance(accountIdInt, newBalance, newTotalDeposit, newTotalWithdraw);

                        if (!isSaved) {
                            System.out.println("CẢNH BÁO: Không thể trừ số dư trong cơ sở dữ liệu!");
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("LỖI: ID tài khoản không hợp lệ.");
                    }
                }

                // 🔥 THÊM GIAO DỊCH VÀO LỊCH SỬ UI
                addTransactionToHistory("Rút tiền thành công", "Ví điện tử / Ngân hàng", amount, false);

                updateWalletUI();
                txtWithdraw.clear();
                showNotify("Thành công", "Đã rút thành công " + formatter.format(amount) + " đ khỏi ví!");
            } else {
                showNotify("Rút tiền thất bại", "Số dư khả dụng trong ví không đủ!");
            }

        } catch (NumberFormatException e) {
            showNotify("Sai định dạng", "Vui lòng chỉ gõ số nguyên, không nhập chữ.");
        }
    }

    /**
     * Hàm tự động vẽ một dòng HBox chứa lịch sử và thêm thẳng vào transactionContainer
     */
    private void addTransactionToHistory(String title, String type, double amount, boolean isDeposit) {
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

        String currentTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblSub = new Label(type + " • " + currentTime);
        lblSub.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        textContainer.getChildren().addAll(lblTitle, lblSub);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label lblAmount = new Label();
        if (isDeposit) {
            lblAmount.setText("+" + formatter.format(amount) + " đ");
            lblAmount.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #059669;");
        } else {
            lblAmount.setText("-" + formatter.format(amount) + " đ");
            lblAmount.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #dc2626;");
        }

        row.getChildren().addAll(circle, textContainer, spacer, lblAmount);
        transactionContainer.getChildren().add(0, row); // add(0, ...) để đẩy giao dịch mới nhất lên hàng đầu
    }

    private void showNotify(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.text.DecimalFormat;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTotalDeposit;
    @FXML private Label lblTotalWithdraw;
    @FXML private TextField txtDeposit;
    @FXML private TextField txtWithdraw;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @FXML
    public void initialize() {
        // Làm mới giao diện ngay khi tab được nạp vào màn hình
        updateWalletUI();
    }

    /**
     * Hàm đồng bộ số liệu siêu an toàn - Đọc thẳng từ các biến tĩnh (Static) của Session Client
     */
    private void updateWalletUI() {
        // ĐỌC THẲNG BIẾN TĨNH: Không thông qua thực thể account.getBalance() nữa để tránh lỗi cache luồng
        double balance = CurrentAccount.getBalance();
        double totalDeposit = CurrentAccount.getTotalDeposit();
        double totalWithdraw = CurrentAccount.getTotalWithdraw();

        System.out.println("LOG UI: Số dư=" + balance + " | Tổng nạp=" + totalDeposit + " | Tổng chi=" + totalWithdraw);

        // 1. Hiển thị số dư khả dụng
        if (balance == 0) {
            lblBalance.setText("0 đ");
        } else {
            lblBalance.setText(formatter.format(balance) + " đ");
        }

        // 2. Hiển thị Tổng nạp
        if (totalDeposit == 0) {
            lblTotalDeposit.setText("0 đ");
        } else {
            lblTotalDeposit.setText(formatter.format(totalDeposit) + " đ");
        }

        // 3. Hiển thị Tổng chi tiêu
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

            // 1. Thực hiện tính toán nạp tiền cục bộ trên RAM Client
            CurrentAccount.deposit(amount);

            // 2. Đẩy số dư mới sau khi nạp xuống database MySQL
            if (CurrentAccount.getAccount() != null) {
                try {
                    int accountIdInt = Integer.parseInt(CurrentAccount.getAccount().getId());
                    double newBalance = CurrentAccount.getBalance();
                    double newTotalDeposit = CurrentAccount.getTotalDeposit();
                    double newTotalWithdraw = CurrentAccount.getTotalWithdraw();

                    // 🔥 ĐỒNG BỘ NGƯỢC LÊN OBJECT RAM: Cập nhật lại thuộc tính của chính object hiện tại
                    CurrentAccount.getAccount().setBalance(newBalance);
                    if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Bidder) {
                        ((com.auction.shared.model.Bidder) CurrentAccount.getAccount()).setTotalDeposit(newTotalDeposit);
                    } else if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Seller) {
                        ((com.auction.shared.model.Seller) CurrentAccount.getAccount()).setTotalDeposit(newTotalDeposit);
                    }

                    // Gọi AccountDAO để thực thi câu lệnh UPDATE vĩnh viễn vào MySQL
                    com.auction.server.dao.AccountDAO accountDAO = new com.auction.server.dao.AccountDAO();
                    boolean isSaved = accountDAO.updateBalance(accountIdInt, newBalance, newTotalDeposit, newTotalWithdraw);

                    if (!isSaved) {
                        System.out.println("CẢNH BÁO: Không thể cập nhật số dư mới vào cơ sở dữ liệu!");
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("LỖI: ID tài khoản không hợp lệ (không phải định dạng số).");
                }
            }

            // 3. Ép giao diện quét lại biến tĩnh và làm sạch ô nhập
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

            // 1. Thực hiện tính toán rút tiền cục bộ trên RAM Client
            boolean isSuccess = CurrentAccount.withdraw(amount);

            if (isSuccess) {
                // 2. Đẩy số dư mới sau khi rút xuống database MySQL vĩnh viễn
                if (CurrentAccount.getAccount() != null) {
                    try {
                        int accountIdInt = Integer.parseInt(CurrentAccount.getAccount().getId());
                        double newBalance = CurrentAccount.getBalance();
                        double newTotalDeposit = CurrentAccount.getTotalDeposit();
                        double newTotalWithdraw = CurrentAccount.getTotalWithdraw();

                        // 🔥 ĐỒNG BỘ NGƯỢC LÊN OBJECT RAM: Cập nhật lại thuộc tính của chính object hiện tại
                        CurrentAccount.getAccount().setBalance(newBalance);
                        if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Bidder) {
                            ((com.auction.shared.model.Bidder) CurrentAccount.getAccount()).setTotalWithdraw(newTotalWithdraw);
                        } else if (CurrentAccount.getAccount() instanceof com.auction.shared.model.Seller) {
                            ((com.auction.shared.model.Seller) CurrentAccount.getAccount()).setTotalWithdraw(newTotalWithdraw);
                        }

                        // Thực thi lệnh UPDATE số dư mới vào MySQL
                        com.auction.server.dao.AccountDAO accountDAO = new com.auction.server.dao.AccountDAO();
                        boolean isSaved = accountDAO.updateBalance(accountIdInt, newBalance, newTotalDeposit, newTotalWithdraw);

                        if (!isSaved) {
                            System.out.println("CẢNH BÁO: Không thể trừ số dư trong cơ sở dữ liệu!");
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("LỖI: ID tài khoản không hợp lệ (không phải định dạng số).");
                    }
                }

                // 3. Cập nhật lại giao diện hiển thị và làm sạch ô nhập
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

    private void showNotify(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
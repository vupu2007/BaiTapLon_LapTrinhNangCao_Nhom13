package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import java.util.*;

public class AccountService {

    private final AccountDAO accountDAO = new AccountDAO();

    // 1. Đăng nhập — trả về đúng loại object theo role
    public Account login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            System.err.println("Username hoặc password không được để trống!");
            return null;
        }

        Account account = accountDAO.login(username, password);

        if (account == null) {
            System.err.println("Sai tên đăng nhập hoặc mật khẩu!");
        } else {
            System.out.println("Đăng nhập thành công: " + username + " [" + account.getRole() + "]");
        }

        return account;
    }

    // 2. Đăng ký — mặc định role BIDDER
    public boolean register(String username, String password, String email) {
        if (username == null || username.isBlank()) {
            System.err.println("Username không được để trống!");
            return false;
        }
        if (password == null || password.length() < 6) {
            System.err.println("Password phải có ít nhất 6 ký tự!");
            return false;
        }
        if (accountDAO.isUsernameExist(username)) {
            System.err.println("Username đã tồn tại!");
            return false;
        }

        return accountDAO.register(username, password, email);
    }

    // 3. Đổi vai trò (BIDDER ↔ SELLER)
    //    Trả về object mới đúng với role sau khi đổi
    public Account switchRole(Account currentAccount) {
        if (currentAccount == null) return null;

        String newRole = currentAccount instanceof Bidder ? "SELLER" : "BIDDER";
        int accountId  = Integer.parseInt(currentAccount.getId());

        boolean success = accountDAO.switchRole(accountId, newRole);
        if (!success) {
            System.err.println("Không thể đổi vai trò!");
            return null;
        }

        // Lấy lại object mới đúng với role vừa đổi
        Account updated = accountDAO.getAccountById(accountId);
        System.out.println("Đã chuyển sang vai trò: " + newRole);
        return updated;
    }

    // 4. Nạp tiền vào ví
    public boolean deposit(Account account, double amount) {
        if (amount <= 0) {
            System.err.println("Số tiền nạp phải lớn hơn 0!");
            return false;
        }
        if (!(account instanceof Bidder || account instanceof Seller)) {
            System.err.println("Admin không có ví tiền!");
            return false;
        }

        int accountId = Integer.parseInt(account.getId());

        // Lấy thông tin tài khoản mới nhất từ database để tránh sai số dư
        Account dbAccount = accountDAO.getAccountById(accountId);
        if (dbAccount == null) return false;

        double currentBalance = 0.0;
        double currentTotalDeposit = 0.0;
        double currentTotalWithdraw = 0.0;

        // Trích xuất dữ liệu tùy theo vai trò
        if (dbAccount instanceof Bidder) {
            currentBalance = ((Bidder) dbAccount).getBalance();
        } else if (dbAccount instanceof Seller) {
            currentBalance = ((Seller) dbAccount).getBalance();
        }

        // Tính toán số dư mới và cộng dồn vào tổng nạp
        double newBalance = currentBalance + amount;
        double newTotalDeposit = currentTotalDeposit + amount; // Cộng dồn số tiền nạp mới vào tổng nạp

        // 🌟 ĐÃ SỬA FULL: Đồng bộ tham số chuẩn chỉnh cho tầng DAO hoạt động
        return accountDAO.updateBalance(accountId, newBalance, newTotalDeposit, currentTotalWithdraw);
    }

    // ── Các method bổ sung cho ClientHandler ─────────────────────────────────

    // Lấy tất cả user dạng Map để Admin hiển thị
    public List<Map<String, String>> getAllUsersAsMap() {
        List<Account> accounts = accountDAO.getAllAccounts();
        List<Map<String, String>> result = new ArrayList<>();
        for (Account acc : accounts) {
            Map<String, String> map = new HashMap<>();
            map.put("id",       acc.getId());
            map.put("username", acc.getUsername());
            map.put("role",     acc.getRole());
            double bal = 0.0;
            if (acc instanceof com.auction.shared.model.Bidder)
                bal = ((com.auction.shared.model.Bidder) acc).getBalance();
            else if (acc instanceof com.auction.shared.model.Seller)
                bal = ((com.auction.shared.model.Seller) acc).getBalance();
            map.put("balance", String.format("%,.0f d", bal));
            map.put("status",  "Dang hoat dong");
            result.add(map);
        }
        return result;
    }

    // Cap nhat thong tin ca nhan
    public boolean updateProfile(String id, String newUsername, String newEmail) {
        if (id == null || newUsername == null || newUsername.isBlank()) return false;
        return accountDAO.updateProfile(Integer.parseInt(id), newUsername, newEmail);
    }

    // Doi mat khau - Server verify current password truoc
    public boolean changePassword(String id, String currentPassword, String newPassword) {
        if (id == null || currentPassword == null || newPassword == null) return false;
        if (newPassword.length() < 6) return false;
        Account acc = accountDAO.getAccountById(Integer.parseInt(id));
        if (acc == null) return false;
        if (!acc.getPassword().equals(currentPassword)) return false;
        return accountDAO.updatePassword(Integer.parseInt(id), newPassword);
    }

    // Lay username theo id (dung cho push realtime sau PLACE_BID)
    public String getUsernameById(String id) {
        try {
            Account acc = accountDAO.getAccountById(Integer.parseInt(id));
            return acc != null ? acc.getUsername() : "An danh";
        } catch (Exception e) {
            return "An danh";
        }
    }

    // 🌟 Giao dịch ví: DEPOSIT hoặc WITHDRAW (ĐÃ SỬA FULL THAM SỐ)
    public boolean walletTransaction(int accountId, double amount, String type) {
        if (amount <= 0) return false;
        Account acc = accountDAO.getAccountById(accountId);
        if (acc == null) return false;

        double currentBalance = 0.0;
        if (acc instanceof com.auction.shared.model.Bidder)
            currentBalance = ((com.auction.shared.model.Bidder) acc).getBalance();
        else if (acc instanceof com.auction.shared.model.Seller)
            currentBalance = ((com.auction.shared.model.Seller) acc).getBalance();

        double newBalance;
        double totalDeposit = 0.0, totalWithdraw = 0.0;

        if ("DEPOSIT".equals(type)) {
            newBalance = currentBalance + amount;
            totalDeposit = amount;
        } else if ("WITHDRAW".equals(type)) {
            if (currentBalance < amount) return false;
            newBalance = currentBalance - amount;
            totalWithdraw = amount;
        } else {
            return false;
        }

        // 🌟 FIX LỖI: Gọi hàm với đầy đủ 4 tham số như AccountDAO yêu cầu
        boolean ok = accountDAO.updateBalance(accountId, newBalance, totalDeposit, totalWithdraw);

        // 🌟 FIX LỖI: Thêm các tham số log cho hàm insertTransaction chạy chuẩn
        if (ok) {
            accountDAO.insertTransaction(accountId, type, amount, newBalance);
        }
        return ok;
    }

    // Lay lich su giao dich vi
    public List<Map<String, Object>> getTransactions(int accountId) {
        return accountDAO.getTransactions(accountId);
    }
}
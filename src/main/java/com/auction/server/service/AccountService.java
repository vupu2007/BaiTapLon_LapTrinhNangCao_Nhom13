package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.shared.exception.AuthenticationException;

public class AccountService {

    private final AccountDAO accountDAO = new AccountDAO();

    // 🌟 KHÓA AN TOÀN NỘI BỘ: Ngăn chặn xung đột luồng khi thay đổi số dư cùng một tài khoản
    private final ConcurrentHashMap<Integer, Object> userLocks = new ConcurrentHashMap<>();

    public Account login(String username, String password) throws AuthenticationException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException(
                    "Username hoặc password không được để trống!",
                    AuthenticationException.Reason.INVALID_CREDENTIALS
            );
        }

        Account account = accountDAO.login(username, password);

        if (account == null) {
            throw new AuthenticationException(
                    "Sai tên đăng nhập hoặc mật khẩu!",
                    AuthenticationException.Reason.INVALID_CREDENTIALS
            );
        }

        System.out.println("✅ Đăng nhập thành công: " + username + " [" + account.getRole() + "]");
        return account;
    }
    /**
     * Xử lý logic quên mật khẩu: Kiểm tra -> Tạo OTP -> Gửi mail
     */
    public boolean processForgotPassword(String username, String email) {
        if (!accountDAO.verifyUserEmail(username, email)) {
            System.err.println("❌ Quên mật khẩu: Không tìm thấy tài khoản với Email này.");
            return false;
        }

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));

        accountDAO.saveOTP(username, otp);

        try {
            com.auction.server.service.EmailService emailService = new com.auction.server.service.EmailService();
            emailService.sendEmail(email, "Mã xác thực khôi phục mật khẩu",
                    "Chào bạn,\n\nMã xác thực để đặt lại mật khẩu của bạn là: " + otp +
                            "\n\nLưu ý: Mã này chỉ có hiệu lực trong thời gian ngắn.");
            System.out.println("📧 Đã gửi OTP cho user: " + username);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi email: " + e.getMessage());
            return false;
        }
    }

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

    public Account switchRole(Account currentAccount) {
        if (currentAccount == null) return null;
        String newRole = currentAccount instanceof Bidder ? "SELLER" : "BIDDER";
        int accountId  = Integer.parseInt(currentAccount.getId());

        Object lock = userLocks.computeIfAbsent(accountId, k -> new Object());
        synchronized (lock) {
            boolean success = accountDAO.switchRole(accountId, newRole);
            if (!success) {
                System.err.println("Không thể đổi vai trò!");
                return null;
            }
            return accountDAO.getAccountById(accountId);
        }
    }

    public boolean deposit(Account account, double amount) {
        if (account == null || amount <= 0) return false;
        if (!(account instanceof Bidder || account instanceof Seller)) {
            System.err.println("Tài khoản quyền quản trị không sở hữu ví tiền tài chính!");
            return false;
        }
        int accountId = Integer.parseInt(account.getId());
        return walletTransaction(accountId, amount, "DEPOSIT");
    }

    public boolean walletTransaction(int accountId, double amount, String type) {
        if (amount <= 0) return false;

        Object lock = userLocks.computeIfAbsent(accountId, k -> new Object());

        synchronized (lock) {
            Account acc = accountDAO.getAccountById(accountId);
            if (acc == null) return false;

            double currentBalance = 0.0;
            if (acc instanceof Bidder) {
                currentBalance = ((Bidder) acc).getBalance();
            } else if (acc instanceof Seller) {
                currentBalance = ((Seller) acc).getBalance();
            }

            if ("WITHDRAW".equals(type) && currentBalance < amount) {
                System.err.println("⚠️ Giao dịch thất bại: Tài khoản #" + accountId + " không đủ số dư!");
                return false;
            }

            boolean ok = accountDAO.executeAtomicWalletUpdate(accountId, amount, type);

            if (ok) {
                Account updated = accountDAO.getAccountById(accountId);
                double balanceAfter = 0;
                if (updated instanceof Bidder) balanceAfter = ((Bidder) updated).getBalance();
                else if (updated instanceof Seller) balanceAfter = ((Seller) updated).getBalance();
                accountDAO.insertTransaction(accountId, type, amount, balanceAfter);
                System.out.println("💰 Ví #" + accountId + " [" + type + "]: " + amount + " đ thành công.");
            }
            return ok;
        }
    }

    public List<Map<String, String>> getAllUsersAsMap() {
        List<Account> accounts = accountDAO.getAllAccounts();
        List<Map<String, String>> result = new ArrayList<>();
        for (Account acc : accounts) {
            Map<String, String> map = new HashMap<>();
            map.put("id",       acc.getId());
            map.put("username", acc.getUsername());
            map.put("role",     acc.getRole());

            double bal = 0.0;
            if (acc instanceof Bidder) bal = ((Bidder) acc).getBalance();
            else if (acc instanceof Seller) bal = ((Seller) acc).getBalance();

            map.put("balance", String.format("%,.0f đ", bal));
            map.put("status", acc.getIsLocked() == 1 ? "BANNED" : "ACTIVE");
            result.add(map);
        }
        return result;
    }

    public boolean updateProfile(String id, String newUsername, String newEmail) {
        if (id == null || newUsername == null || newUsername.isBlank()) return false;
        return accountDAO.updateProfile(Integer.parseInt(id), newUsername, newEmail);
    }

    public boolean changePassword(String id, String currentPassword, String newPassword) {
        if (id == null || currentPassword == null || newPassword == null) return false;
        if (newPassword.length() < 6) return false;

        int accId = Integer.parseInt(id);
        Object lock = userLocks.computeIfAbsent(accId, k -> new Object());
        synchronized (lock) {
            Account acc = accountDAO.getAccountById(accId);
            if (acc == null) return false;
            if (!acc.getPassword().equals(currentPassword)) return false;
            // ĐÃ SỬA: Gọi đúng hàm Raw với tham số int
            return accountDAO.updatePasswordRaw(accId, newPassword);
        }
    }

    public String getUsernameById(String id) {
        try {
            Account acc = accountDAO.getAccountById(Integer.parseInt(id));
            return acc != null ? acc.getUsername() : "Ẩn danh";
        } catch (Exception e) {
            return "Ẩn danh";
        }
    }

    public List<Map<String, Object>> getTransactions(int accountId) {
        return accountDAO.getTransactions(accountId);
    }
}
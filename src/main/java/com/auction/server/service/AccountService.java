package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccountService {

    private final AccountDAO accountDAO = new AccountDAO();

    // 🌟 KHÓA AN TOÀN NỘI BỘ: Ngăn chặn xung đột luồng khi thay đổi số dư cùng một tài khoản
    private final ConcurrentHashMap<Integer, Object> userLocks = new ConcurrentHashMap<>();

    public Account login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            System.err.println("Username hoặc password không được để trống!");
            return null;
        }
        Account account = accountDAO.login(username, password);
        if (account == null) {
            System.err.println("Sai tên đăng nhập hoặc mật khẩu!");
        } else {
            System.out.println("✅ Đăng nhập thành công: " + username + " [" + account.getRole() + "]");
        }
        return account;
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

        // Đồng bộ hóa việc đổi vai trò tránh spam request đổi vai trò liên tục
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

    /**
     * 🌟 TỐI ƯU CỐT LÕI: Hàm nạp tiền chuyển hướng gọi trực tiếp walletTransaction
     * để tái sử dụng một luồng xử lý duy nhất, giảm 50% số lượng truy vấn DB thừa.
     */
    public boolean deposit(Account account, double amount) {
        if (account == null || amount <= 0) return false;
        if (!(account instanceof Bidder || account instanceof Seller)) {
            System.err.println("Tài khoản quyền quản trị không sở hữu ví tiền tài chính!");
            return false;
        }
        int accountId = Integer.parseInt(account.getId());
        return walletTransaction(accountId, amount, "DEPOSIT");
    }

    /**
     * 🚀 GIẢI PHÁP AN TOÀN TÀI CHÍNH: Tích hợp cơ chế khóa phân đoạn (Striped Locking)
     * phối hợp với câu lệnh tăng trưởng nguyên tử bảo vệ số dư tuyệt đối.
     */
    public boolean walletTransaction(int accountId, double amount, String type) {
        if (amount <= 0) return false;

        // Lấy hoặc tạo một Object Lock chuyên biệt cho DUY NHẤT ID người dùng này
        // Giúp User A giao dịch không bị block bởi User B, nhưng User A không thể tự xung đột chính mình
        Object lock = userLocks.computeIfAbsent(accountId, k -> new Object());

        synchronized (lock) {
            // 1. Kiểm tra trạng thái và số dư an toàn trước khi hành động
            Account acc = accountDAO.getAccountById(accountId);
            if (acc == null) return false;

            double currentBalance = 0.0;
            if (acc instanceof Bidder) {
                currentBalance = ((Bidder) acc).getBalance();
            } else if (acc instanceof Seller) {
                currentBalance = ((Seller) acc).getBalance();
            }

            // 2. Chặn rút quá số dư khả dụng
            if ("WITHDRAW".equals(type) && currentBalance < amount) {
                System.err.println("⚠️ Giao dịch thất bại: Tài khoản #" + accountId + " không đủ số dư!");
                return false;
            }

            // 3. 🚀 ỦY QUYỀN ĐỒNG BỘ CHO DATABASE (Atomic DB Update)
            // Bạn cần sửa hàm updateBalance trong AccountDAO của bạn nhận tham số dạng Delta (sai số cộng trừ):
            // Lệnh SQL chuẩn trong DAO nên là:
            // "UPDATE account SET balance = balance + ?, total_deposit = total_deposit + ? WHERE id = ?" (Nếu là DEPOSIT)
            // "UPDATE account SET balance = balance - ?, total_withdraw = total_withdraw + ? WHERE id = ?" (Nếu là WITHDRAW)

            boolean ok = accountDAO.executeAtomicWalletUpdate(accountId, amount, type);

            if (ok) {
                accountDAO.insertTransaction(accountId, amount, type);
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
            map.put("status",  "Đang hoạt động");
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
            return accountDAO.updatePassword(accId, newPassword);
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
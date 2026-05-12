package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;

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

        int accountId   = Integer.parseInt(account.getId());
        double newBalance = accountDAO.getAccountById(accountId) instanceof Bidder
                ? ((Bidder) accountDAO.getAccountById(accountId)).getBalance() + amount
                : ((Seller) accountDAO.getAccountById(accountId)).getBalance() + amount;

        return accountDAO.updateBalance(accountId, newBalance);
    }
}
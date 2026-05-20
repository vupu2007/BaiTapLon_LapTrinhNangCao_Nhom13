package com.auction.client.util;

import com.auction.shared.model.Account;

/**
 * Class này dùng để lưu trữ thông tin tài khoản hiện đang đăng nhập vào hệ thống.
 */
public class CurrentAccount {
    private static Account account;

    // QUAN TRỌNG: Khai báo các biến tĩnh để quản lý số dư và tích lũy trực tiếp tại phiên Client
    private static Double sessionBalance = 0.0;
    private static double totalDeposit = 0.0;
    private static double totalWithdraw = 0.0;

    // Khi đăng nhập thành công, gọi hàm này để "ghi nhớ" tài khoản
    public static void setAccount(Account loggedInAccount) {
        account = loggedInAccount;

        if (account != null) {
            // Lấy số dư thực tế được truyền từ DB sang đối tượng Account
            sessionBalance = account.getBalance() != null ? account.getBalance() : 0.0;

            // Đọc và giữ lại tổng nạp / tổng chi từ đối tượng Account đã có data
            if (account instanceof com.auction.shared.model.Bidder) {
                totalDeposit = ((com.auction.shared.model.Bidder) account).getTotalDeposit();
                totalWithdraw = ((com.auction.shared.model.Bidder) account).getTotalWithdraw();
            } else if (account instanceof com.auction.shared.model.Seller) {
                totalDeposit = ((com.auction.shared.model.Seller) account).getTotalDeposit();
                totalWithdraw = ((com.auction.shared.model.Seller) account).getTotalWithdraw();
            }
        } else {
            sessionBalance = 0.0;
            totalDeposit = 0.0;
            totalWithdraw = 0.0;
        }
    }

    private CurrentAccount() {
    }

    public static Account getAccount() {
        return account;
    }

    public static void logOut() {
        account = null;
        sessionBalance = 0.0;
        totalDeposit = 0.0;
        totalWithdraw = 0.0;
    }

    // ================= LOGIC VÍ TIỀN ĐÃ ĐỒNG BỘ ĐỘC LẬP =================

    /**
     * Lấy số dư hiện tại từ biến phiên (Sửa lỗi hiển thị 0đ)
     */
    public static Double getBalance() {
        return sessionBalance;
    }

    public static double getTotalDeposit() {
        return totalDeposit;
    }

    public static double getTotalWithdraw() {
        return totalWithdraw;
    }

    /**
     * Logic cộng tiền khi nạp tiền thành công
     */
    public static void deposit(double amount) {
        if (amount > 0) {
            // 1. Cộng dồn vào biến số dư phiên
            sessionBalance += amount;

            // 2. Đồng bộ ngược lại vào đối tượng account (nếu cần dùng nơi khác)
            if (account != null) {
                account.setBalance(sessionBalance);
            }

            // 3. Tích lũy vào tổng nạp
            totalDeposit += amount;

            System.out.println("DEBUG: Nạp tiền thành công. Số dư hiện tại trên RAM: " + sessionBalance);
        }
    }

    /**
     * Logic trừ tiền khi rút tiền
     */
    public static boolean withdraw(double amount) {
        if (amount > 0 && sessionBalance >= amount) {
            // 1. Trừ bớt tiền ở số dư phiên
            sessionBalance -= amount;

            // 2. Đồng bộ ngược lại vào đối tượng account
            if (account != null) {
                account.setBalance(sessionBalance);
            }

            // 3. Tích lũy vào tổng chi
            totalWithdraw += amount;

            System.out.println("DEBUG: Rút tiền thành công. Số dư hiện tại trên RAM: " + sessionBalance);
            return true;
        }
        return false;
    }
}
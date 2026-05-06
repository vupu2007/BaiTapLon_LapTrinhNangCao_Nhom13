package com.auction.util;

import com.auction.model.Account;

/**
 * Class này dùng để lưu trữ thông tin người dùng hiện đang đăng nhập vào hệ thống.
 */
public class CurrentAccount {

    // SỬA 1: Kiểu dữ liệu phải là Account (để chứa Admin/Bidder/Seller)
    // Đổi tên biến thành 'account' cho ngắn gọn và tránh nhầm với tên Class
    private static Account account;

    // Khi đăng nhập thành công, gọi hàm này để "ghi nhớ" Account
    public static void setAccount(Account loggedInAccount) {
        // SỬA 2: Gán vào biến 'account' ở trên, không gán vào tên Class 'CurrentAccount'
        account = loggedInAccount;
    }

    // SỬA 3: Kiểu trả về phải là Account để các Controller khác sử dụng được thông tin
    public static Account getAccount() {
        return account;
    }

    // Khi người dùng nhấn "Đăng xuất"
    public static void logOut() {
        account = null;
    }
}
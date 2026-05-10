package com.auction.util;

import com.auction.model.Account; // Import Account thay vì User

/**
 * Class này dùng để lưu trữ thông tin tài khoản hiện đang đăng nhập vào hệ thống.
 */
public class CurrentAccount {
    // Kiểu dữ liệu phải là Account (để chứa Admin/Bidder/Seller)
    // Đổi tên biến thành 'account' cho ngắn gọn và tránh nhầm với tên Class
    private static Account account;

    // Khi đăng nhập thành công, gọi hàm này để "ghi nhớ" tài khoản
    public static void setAccount(Account loggedInAccount) {
        //  Gán vào biến 'account' ở trên, không gán vào tên Class 'CurrentAccount'
        account = loggedInAccount;
    }
    private CurrentAccount() {
    }

    // Khi cần lấy thông tin, gọi hàm này
    public static Account getAccount() {
        return account;
    }

    // Khi người dùng nhấn "Đăng xuất"
    public static void logOut() {
        account = null;
    }
}
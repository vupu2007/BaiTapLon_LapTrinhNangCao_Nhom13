package com.auction.util;

import com.auction.model.Account; // Import Account thay vì User

/**
 * Class này dùng để lưu trữ thông tin tài khoản hiện đang đăng nhập vào hệ thống.
 */
public class CurrentUser {
    // Đổi kiểu dữ liệu từ User sang Account để chứa được cả Admin và Bidder/Seller
    private static Account account;

    // Khi đăng nhập thành công, gọi hàm này để "ghi nhớ" tài khoản
    public static void setUser(Account loggedInAccount) {
        account = loggedInAccount;
    }

    // Khi cần lấy thông tin, gọi hàm này
    public static Account getUser() {
        return account;
    }

    // Khi người dùng nhấn "Đăng xuất"
    public static void logOut() {
        account = null;
    }
}
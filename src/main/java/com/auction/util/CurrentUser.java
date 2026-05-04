package com.auction.util;

import com.auction.model.User;

/**
 * Class này dùng để lưu trữ thông tin người dùng hiện đang đăng nhập vào hệ thống.
 * Nó giúp các Controller khác có thể lấy thông tin User mà không cần truyền dữ liệu qua lại phức tạp.
 */
public class CurrentUser {
    // Biến static để giữ thông tin User trong suốt quá trình App chạy
    private static User user;

    // Khi đăng nhập thành công, gọi hàm này để "ghi nhớ" User
    public static void setUser(User loggedInUser) {
        user = loggedInUser;
    }

    // Khi cần lấy thông tin (ví dụ: lấy ID để đặt giá), gọi hàm này
    public static User getUser() {
        return user;
    }

    // Khi người dùng nhấn "Đăng xuất"
    public static void logOut() {
        user = null;
    }
}
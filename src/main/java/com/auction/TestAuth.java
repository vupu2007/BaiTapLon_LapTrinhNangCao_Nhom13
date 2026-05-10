package com.auction;

import com.auction.model.Account;
import com.auction.service.AccountService;

public class TestAuth {
    public static void main(String[] args) {

        AccountService service = new AccountService();

        // --- Test đăng ký ---
        System.out.println("=== TEST ĐĂNG KÝ ===");
        boolean registered = service.register("testuser", "123456", "test@gmail.com");
        System.out.println("Kết quả đăng ký: " + (registered ? "THÀNH CÔNG" : "THẤT BẠI"));

        // --- Test đăng nhập đúng ---
        System.out.println("\n=== TEST ĐĂNG NHẬP ĐÚNG ===");
        Account acc = service.login("testuser", "123456");
        if (acc != null) {
            System.out.println("Đăng nhập thành công!");
            System.out.println("Username: " + acc.getUsername());
            System.out.println("Role: "     + acc.getRole());
        }

        // --- Test đăng nhập sai mật khẩu ---
        System.out.println("\n=== TEST ĐĂNG NHẬP SAI ===");
        Account acc2 = service.login("testuser", "saimatkhau");
        System.out.println("Kết quả: " + (acc2 == null ? "Đúng — trả về null" : "Sai — không lẽ đăng nhập được?"));
    }
}
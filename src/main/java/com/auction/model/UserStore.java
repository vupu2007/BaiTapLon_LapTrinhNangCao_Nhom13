package com.auction.model;

import java.util.HashMap;
import java.util.Map;

public class UserStore {
    // Lưu tài khoản dưới dạng: <Tên đăng nhập, Mật khẩu>
    public static Map<String, String> users = new HashMap<>();

    static {
        // Tạo sẵn 2 tài khoản demo
        users.put("buyer", "123");
        users.put("seller", "123");
    }
}
package com.auction.service;
import com.auction.model.Bidder;
import com.auction.model.User;

public class UserService {

    public boolean register(String username, String password, String role) {
        if (username == null || username.isEmpty()
                || password == null || password.isEmpty()
                || role == null) {
            return false;
        }

        System.out.println("Đã tạo user: " + username + " role: " + role);

        return true;
    }

    public User login(String username, String password) {
        if ("testuser".equals(username) && "123456".equals(password)) {
            // Giả sử đăng nhập xong thì mặc định là Bidder
            // Phải truyền đủ id, username, password vào constructor
            User user = new Bidder("1", username, password);
            return user;
        }
        return null;
    }
}
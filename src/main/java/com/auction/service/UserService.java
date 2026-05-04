package com.auction.service;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    /**
     * Hàm đăng ký người dùng mới vào Database
     */
    public boolean register(String username, String password, String email, String role) {
        // 1. Kiểm tra đầu vào cơ bản
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        // 2. Câu lệnh SQL để thêm User
        String sql = "INSERT INTO Users (username, password, email, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.setString(4, role.toUpperCase()); // Đảm bảo role là ADMIN hoặc USER

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Đã lưu user mới vào DB: " + username);
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi đăng ký: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hàm đăng nhập kiểm tra thông tin từ Database
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Lấy thông tin từ Database
                String id = String.valueOf(rs.getInt("user_id"));
                String role = rs.getString("role");

                System.out.println("Đăng nhập thành công: " + username + " (Role: " + role + ")");

                // Trả về đúng loại đối tượng theo Model của bạn
                if ("ADMIN".equalsIgnoreCase(role)) {
                    return new Admin(id, username, password);
                } else {
                    return new Bidder(id, username, password);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đăng nhập: " + e.getMessage());
        }

        System.out.println("Sai tài khoản hoặc mật khẩu!");
        return null;
    }
}
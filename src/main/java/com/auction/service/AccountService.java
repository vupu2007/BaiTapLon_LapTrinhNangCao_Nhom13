package com.auction.service;

import com.auction.model.*;
import com.auction.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountService {

    /**
     * Hàm đăng nhập: Trả về đối tượng thuộc kiểu User (Cha)
     */
    public Account login(String username, String password) {
         String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String id = String.valueOf(rs.getInt("user_id"));
                String role = rs.getString("role");

                System.out.println("Vibe thành công: " + username + " [" + role + "]");

                // Trả về đúng loại đối tượng con dựa trên Role
                if ("ADMIN".equalsIgnoreCase(role)) {
                    // Admin không cần tiền, chỉ cần 4 tham số
                    return new Admin(id, username, password, role);

                } else {
                    // SỬA 2: Lấy tiền từ DB ra cho Seller và Bidder
                    // (Hãy đảm bảo trong bảng user của bạn có cột tên là "balance")
                    double balance = rs.getDouble("balance");

                    if ("SELLER".equalsIgnoreCase(role)) {
                        // Seller cần 5 tham số (có thêm balance)
                        return new Seller(id, username, password, role, balance);
                    } else {
                        // Bidder cũng cần 5 tham số
                        return new Bidder(id, username, password, role, balance);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi logic Backend: " + e.getMessage());
        }
        return null;
    }

    /**
     * Hàm đăng ký người dùng mới
     */
    public boolean register(String username, String password, String role) {
        String sql = "INSERT INTO Users (username, password, role) VALUES ( ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.toUpperCase());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo tài khoản: " + e.getMessage());
            return false;
        }
    }
}
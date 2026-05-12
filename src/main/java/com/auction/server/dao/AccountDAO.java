package com.auction.server.dao;

import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import com.auction.server.util.DatabaseConnection;
import java.sql.*;

public class AccountDAO {

    // 1. Đăng ký tài khoản mới (mặc định role = BIDDER)
    public boolean register(String username, String password, String email) {
        String sql = "INSERT INTO Accounts (username, password, email, role) VALUES (?, ?, ?, 'BIDDER')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Đăng nhập — trả về đúng loại object theo role
    public Account login(String username, String password) {
        String sql = "SELECT * FROM Accounts WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAccount(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. Lấy tài khoản theo ID
    public Account getAccountById(int accountId) {
        String sql = "SELECT * FROM Accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAccount(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. Đổi vai trò (BIDDER ↔ SELLER)
    public boolean switchRole(int accountId, String newRole) {
        String sql = "UPDATE Accounts SET role = ? WHERE account_id = ? AND role != 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole);
            pstmt.setInt(2, accountId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. Cập nhật số dư ví
    public boolean updateBalance(int accountId, double newBalance) {
        String sql = "UPDATE Accounts SET balance = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newBalance);
            pstmt.setInt(2, accountId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. Kiểm tra username đã tồn tại chưa (dùng khi đăng ký)
    public boolean isUsernameExist(String username) {
        String sql = "SELECT account_id FROM Accounts WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Helper: map ResultSet sang đúng loại Account theo role ---
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        String id       = String.valueOf(rs.getInt("account_id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email    = rs.getString("email");
        String role     = rs.getString("role");
        double balance  = rs.getDouble("balance");

        switch (role) {
            case "ADMIN":  return new Admin(id, username, password, email);
            case "SELLER": return new Seller(id, username, password, email, balance);
            default:       return new Bidder(id, username, password, email, balance);
        }
    }
    //---Hàm đổi mật khẩu--
    public boolean updatePassword(String userId, String newPassword) {
        String sql = "UPDATE Accounts SET password = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, Integer.parseInt(userId));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }
    //-- Hàm đổi profile--
    public boolean updateProfile(String userId, String newUsername, String newEmail) {
        String sql = "UPDATE Accounts SET username = ?, email = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUsername);
            pstmt.setString(2, newEmail);
            pstmt.setInt(3, Integer.parseInt(userId));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }
}
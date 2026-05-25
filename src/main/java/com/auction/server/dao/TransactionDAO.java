package com.auction.server.dao;

import com.auction.server.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class TransactionDAO {

    public boolean save(int accountId, String type, double amount, String description) {
        String sql = "INSERT INTO Transactions (account_id, type, amount, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<Map<String, Object>> getByAccount(int accountId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM Transactions WHERE account_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("type", rs.getString("type"));
                    row.put("amount", rs.getDouble("amount"));
                    row.put("description", rs.getString("description"));

                    // 🛠️ FIX LỖI: Kiểm tra an toàn trước khi ép kiểu tránh sập luồng mạng
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        row.put("created_at", timestamp.toLocalDateTime());
                    } else {
                        row.put("created_at", java.time.LocalDateTime.now());
                    }
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getByAccount: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public boolean insertTransaction(int accountId, String type, double amount, double balanceAfter) {
        String sql = "INSERT INTO Transactions (account_id, type, amount, balance_after, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setDouble(4, balanceAfter);
            ps.setString(5, type.equals("DEPOSIT") ? "Chuyển khoản" : "Ví điện tử / Ngân hàng");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
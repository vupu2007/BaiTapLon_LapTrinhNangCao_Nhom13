package com.auction.server.dao;

import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import com.auction.server.util.DatabaseConnection;
import java.sql.*;

public class AccountDAO {

    // 🔥 HÀM KHỞI TẠO TỰ ĐỘNG (CONSTRUCTOR): Ép MySQL tự động thêm 2 cột nếu chưa có, tránh lỗi sập app vĩnh viễn
    public AccountDAO() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Ép MySQL tạo cột total_deposit nếu bảng Accounts chưa có
            try {
                stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN total_deposit DECIMAL(15,2) DEFAULT 0.0");
                System.out.println("✅ [DATABASE] Đã tự động kiểm tra/thêm cột total_deposit thành công.");
            } catch (SQLException e) {
                // Nếu cột đã tồn tại từ trước, MySQL báo lỗi trùng -> Java chủ động bỏ qua tại đây (An toàn)
            }

            // Ép MySQL tạo cột total_withdraw nếu bảng Accounts chưa có
            try {
                stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN total_withdraw DECIMAL(15,2) DEFAULT 0.0");
                System.out.println("✅ [DATABASE] Đã tự động kiểm tra/thêm cột total_withdraw thành công.");
            } catch (SQLException e) {
                // Nếu cột đã tồn tại từ trước, Java chủ động bỏ qua
            }

        } catch (SQLException e) {
            System.err.println("⚠️ Cảnh báo khởi tạo cấu trúc DB: " + e.getMessage());
        }
    }

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
    public boolean updateBalance(int accountId, double newBalance, double totalDeposit, double totalWithdraw) {
        String sql = "UPDATE Accounts SET balance = ?, total_deposit = ?, total_withdraw = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newBalance);
            pstmt.setDouble(2, totalDeposit);
            pstmt.setDouble(3, totalWithdraw);
            pstmt.setInt(4, accountId);

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

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        String id       = String.valueOf(rs.getInt("account_id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email    = rs.getString("email");
        String role     = rs.getString("role");
        double balance  = rs.getDouble("balance");

        // Khởi tạo giá trị an toàn mặc định
        double totalDeposit = 0.0;
        double totalWithdraw = 0.0;

        // BỌC THỬ AN TOÀN: Đọc dữ liệu thực tế từ MySQL
        try {
            totalDeposit = rs.getDouble("total_deposit");
            totalWithdraw = rs.getDouble("total_withdraw");
        } catch (SQLException e) {
            System.err.println("⚠️ CẢNH BÁO: Database MySQL hiện tại chưa được cập nhật cột total_deposit hoặc total_withdraw!");
        }

        Account acc;
        switch (role) {
            case "ADMIN":
                acc = new Admin(id, username, password, email);
                break;

            case "SELLER":
                Seller seller = new Seller(id, username, password, email, balance);
                seller.setTotalDeposit(totalDeposit);
                seller.setTotalWithdraw(totalWithdraw);
                acc = seller;
                break;

            default: // BIDDER
                Bidder bidder = new Bidder(id, username, password, email, balance);
                bidder.setTotalDeposit(totalDeposit);
                bidder.setTotalWithdraw(totalWithdraw);
                acc = bidder;
                break;
        }

        if (acc != null) {
            acc.setBalance(balance);
        }
        return acc;
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
package com.auction.server.dao;

import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import com.auction.shared.model.Transaction;
import com.auction.server.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class AccountDAO {

    // 1. Đăng ký tài khoản mới (mặc định role = BIDDER)
    public boolean register(String username, String password, String email) {
        String sql = "INSERT INTO Accounts (username, password, email, role) VALUES (?, ?, ?, 'BIDDER')";
        // ✅ ĐÃ SỬA: Đưa conn vào try() để tự động giải phóng về Pool
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
        // Check is_locked trước
        String checkSql = "SELECT is_locked FROM Accounts WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getBoolean("is_locked")) {
                throw new RuntimeException("ACCOUNT_LOCKED");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "SELECT * FROM Accounts WHERE username = ? AND password = ? AND is_locked = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToAccount(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Lưu mã OTP vào database (tạm thời để đối chiếu)
    public void saveOTP(String username, String otp) {
        String sql = "UPDATE Accounts SET reset_code = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi lưu OTP vào Database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean verifyOTP(String username, String otp) {
        String sql = "SELECT account_id FROM Accounts WHERE username = ? AND reset_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, otp);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Nếu tìm thấy hàng, OTP đúng
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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

    // HÀM MỚI: Dùng cho Quên mật khẩu (xác thực bằng username và xóa reset_code)
    public boolean updatePasswordByUsername(String username, String newPassword) {
        String sql = "UPDATE Accounts SET password = ?, reset_code = NULL WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cập nhật mật khẩu: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public void clearOTP(String username) {
        String sql = "UPDATE Accounts SET reset_code = NULL WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 8. Cập nhật thông tin cá nhân (Profile)
    public boolean updateProfile(int accountId, String newUsername, String newEmail) {
        String sql = "UPDATE Accounts SET username = ?, email = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername);
            pstmt.setString(2, newEmail);
            pstmt.setInt(3, accountId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 9. Khóa người dùng (Dành cho Quản trị viên Admin)
    public boolean lockUser(int accountId) {
        String sql = "UPDATE Accounts SET is_locked = 1 WHERE account_id = ? AND role != 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 10. Lấy tất cả danh sách user (Dùng cho AdminUserMgmt)
    public List<Account> getAllAccounts() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM Accounts ORDER BY account_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =========================================================================
    // 🚀 TẦNG NGHIỆP VỤ: XỬ LÝ LỊCH SỬ GIAO DỊCH VÍ ĐỒNG BỘ MODEL
    // =========================================================================

    /**
     * Ghi một dòng lịch sử nạp/rút tiền thật vào bảng Transactions trong Database
     */
    public boolean insertTransaction(int accountId, String type, double amount, double balanceAfter) {
        // 🚀 ĐÃ FIX: Chữ "transactions" viết thường để tương thích với Linux Clever Cloud
        String sql = "INSERT INTO Transactions (account_id, type, amount, balance_after, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.setDouble(4, balanceAfter);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("LỖI GHI LỊCH SỬ GIAO DỊCH: " + e.getMessage()); // In lỗi rõ ràng để dễ bắt
            return false;
        }
    }

    /**
     * Lấy danh sách lịch sử giao dịch từ Database trả về đối tượng Model Transaction nguyên bản
     */
    public List<Transaction> getTransactionHistory(int accountId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM Transactions WHERE account_id = ? ORDER BY transaction_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction();
                    t.setTransactionId(rs.getInt("transaction_id"));
                    t.setAccountId(rs.getInt("account_id"));
                    t.setType(rs.getString("type"));
                    t.setAmount(rs.getDouble("amount"));
                    t.setBalanceAfter(rs.getDouble("balance_after"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        t.setCreatedAt(ts.toLocalDateTime());
                    }
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =========================================================================
    // 🔗 HÀM KẾT NỐI ĐỒNG BỘ MỚI: ĐÁP ỨNG MESSAGE_TYPE.GET_TRANSACTIONS CHO CLIENT
    // =========================================================================

    public List<Map<String, Object>> getTransactions(int accountId) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<Transaction> history = getTransactionHistory(accountId);

        if (history != null) {
            for (Transaction t : history) {
                Map<String, Object> txMap = new HashMap<>();
                txMap.put("type", t.getType());

                String desc = "DEPOSIT".equalsIgnoreCase(t.getType()) ? "Chuyển khoản / Nạp tiền" : "Ví điện tử / Rút tiền";
                txMap.put("description", desc);
                txMap.put("amount", t.getAmount());
                txMap.put("created_at", t.getCreatedAt());

                resultList.add(txMap);
            }
        }
        return resultList;
    }

    // =========================================================================
    // 🔀 CÁC HÀM OVERLOAD TƯƠNG THÍCH ĐỂ KHÔNG LÀM LỖI CODE CŨ CỦA DỰ ÁN
    // =========================================================================

    public boolean executeAtomicWalletUpdate(int accountId, double amount, String type) {
        String sql = "DEPOSIT".equals(type)
                ? "UPDATE Accounts SET balance = balance + ?, total_deposit = total_deposit + ? WHERE account_id = ?"
                : "UPDATE Accounts SET balance = balance - ?, total_withdraw = total_withdraw + ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setDouble(2, amount);
            pstmt.setInt(3, accountId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cập nhật ví: " + e.getMessage());
            return false;
        }
    }

    public boolean updateProfile(String userId, String newUsername, String newEmail) {
        return updateProfile(Integer.parseInt(userId), newUsername, newEmail);
    }

    public String getUsernameById(String id) {
        String sql = "SELECT username FROM Accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Bot_" + id;
    }

    // 1. HÀM GỐC: Thực hiện SQL (Dùng int cho chuẩn Database)
    public boolean updatePasswordRaw(int accountId, String newPassword) {
        String sql = "UPDATE Accounts SET password = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, accountId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. HÀM HỖ TRỢ: Dành cho khi bạn chỉ có String userId (từ request gửi lên)
    public boolean updatePassword(String userId, String newPassword) {
        // Chuyển đổi String sang int rồi gọi hàm Gốc
        return updatePasswordRaw(Integer.parseInt(userId), newPassword);
    }

    public boolean insertTransaction(int accountId, double amount, String type) {
        String sql = "INSERT INTO Transactions (account_id, type, amount, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.setObject(4, java.time.LocalDateTime.now());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi ghi log giao dịch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Helper map dữ liệu sạch từ ResultSet MySQL lên Object Java
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        String id       = String.valueOf(rs.getInt("account_id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email    = rs.getString("email");
        String role     = rs.getString("role");
        double balance  = rs.getDouble("balance");

        double totalDeposit = 0.0;
        double totalWithdraw = 0.0;

        try {
            totalDeposit = rs.getDouble("total_deposit");
            totalWithdraw = rs.getDouble("total_withdraw");
        } catch (SQLException e) {
            System.err.println("⚠️ CẢNH BÁO: Database MySQL chưa có cột total_deposit hoặc total_withdraw!");
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
            default:
                Bidder bidder = new Bidder(id, username, password, email, balance);
                bidder.setTotalDeposit(totalDeposit);
                bidder.setTotalWithdraw(totalWithdraw);
                acc = bidder;
                break;
        }

        if (acc != null) {
            acc.setBalance(balance);
            try { acc.setIsLocked(rs.getInt("is_locked")); } catch (SQLException ignored) {}

        }
        return acc;
    }
    public boolean updateUserStatus(int accountId, String status) {
        String sql = "UPDATE Accounts SET is_locked = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, "BANNED".equals(status) ? 1 : 0);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean isLocked(int accountId) {
        String sql = "SELECT is_locked FROM Accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("is_locked");
        } catch (SQLException ignored) {}
        return false;
    }
    public boolean verifyUserEmail(String username, String email) {
        String sql = "SELECT account_id FROM Accounts WHERE username = ? AND email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
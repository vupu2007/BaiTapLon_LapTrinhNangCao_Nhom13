package com.auction;

import com.auction.server.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestAuth {
    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU KIỂM TRA KẾT NỐI DATABASE ===");

        // Bước 1: Kiểm tra xem Driver đã được nạp vào project chưa
        try {
            // Nếu bạn dùng MariaDB:
            Class.forName("org.mariadb.jdbc.Driver");
            System.out.println("[OK] 1. Tìm thấy MariaDB Driver trong thư viện.");

            // Hoặc nếu bạn dùng MySQL (hãy bỏ comment dòng dưới nếu dùng MySQL):
            // Class.forName("com.mysql.cj.jdbc.Driver");
            // System.out.println("[OK] 1. Tìm thấy MySQL Driver trong thư viện.");
        } catch (ClassNotFoundException e) {
            System.err.println("[THẤT BẠI] 1. Chưa thêm Driver (file .jar) vào thư mục lib hoặc Maven/Gradle!");
            e.printStackTrace();
            return; // Dừng kiểm tra vì thiếu thư viện thì không chạy tiếp được
        }

        // Bước 2: Kiểm tra kết nối vật lý tới Database Server
        System.out.println("\nĐang cố gắng kết nối tới database...");
        try (Connection conn = DatabaseConnection.getConnection()) {

            if (conn != null && !conn.isClosed()) {
                System.out.println("[OK] 2. Kết nối thành công tới Database Server!");
                System.out.println("-> Thấu tin kết nối: " + conn.getMetaData().getURL());

                // Bước 3: Chạy thử một câu lệnh SQL cơ bản để xem DB có thực sự hoạt động
                System.out.println("\n[OK] 3. Thử nghiệm thực thi câu lệnh SQL ẩn...");
                try (PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[OK] -> DB phản hồi tốt! Lệnh 'SELECT 1' trả về: " + rs.getInt(1));
                        System.out.println("\n===> KẾT LUẬN: DATABASE HOÀN TOÀN BÌNH THƯỜNG!");
                    }
                } catch (SQLException e) {
                    System.err.println("[THẤT BẠI] -> Kết nối được nhưng không thể thực thi câu lệnh SQL.");
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            System.err.println("\n[THẤT BẠI] 2. Không thể kết nối tới Database!");
            System.err.println("=== CHI TIẾT MÃ LỖI (SQL State): " + e.getSQLState() + " ===");
            System.err.println("=== CHI TIẾT LỖI (Error Code): " + e.getErrorCode() + " ===");
            System.err.println("=== TIN NHẮN LỖI: " + e.getMessage() + " ===");
            System.err.println("\n--- Vết lỗi chi tiết (Stack Trace) ---");
            e.printStackTrace();

            System.err.println("\n===> GỢI Ý CÁCH SỬA DỰA TRÊN MÃ LỖI:");
        }
    }

    private static void gợiySửaLỗi(String errorMessage) {
        if (errorMessage.contains("Access denied")) {
            System.err.println("-> LỖI TÀI KHOẢN: Sai USER hoặc PASSWORD (kiểm tra lại 'root' và mật khẩu).");
        } else if (errorMessage.contains("Communications link failure") || errorMessage.contains("Connection refused")) {
            System.err.println("-> LỖI KẾT NỐI: Có thể bạn chưa bật XAMPP / Laragon / MySQL Server, hoặc sai PORT (3306).");
        } else if (errorMessage.contains("Unknown database")) {
            System.err.println("-> LỖI TÊN DB: Tên database 'online_auction_db' không tồn tại. Hãy vào phpMyAdmin/HeidiSQL tạo database này trước.");
        } else {
            System.err.println("-> Hãy đọc kỹ nội dung 'TIN NHẮN LỖI' phía trên hoặc gửi đoạn log đó cho mình xem nhé!");
        }
    }
}
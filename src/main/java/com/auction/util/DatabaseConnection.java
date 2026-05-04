package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseConnection {
    // 1. Sửa URL: Chỉ để đến localhost:3306/
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/";
    // 2. Sửa DB_NAME: Phải là auction_system để khớp với Workbench của bạn
    private static final String DB_NAME = "auction_system";
    private static final String USER = "root";
    // 3. ĐIỀN MẬT KHẨU CỦA BẠN VÀO ĐÂY (Giống bên JDBCUtil)
    private static final String PASSWORD = "123456";

    public static Connection getConnection() throws SQLException {
        try {
            // Đăng ký driver (Cần thiết cho một số phiên bản JDBC cũ hơn)
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());

            // Bước 1: Kết nối đến Server để đảm bảo DB tồn tại
            Connection serverConn = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
            Statement stmt = serverConn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.close();
            serverConn.close();

            // Bước 2: Kết nối chính thức vào Database auction_system
            String fullUrl = SERVER_URL + DB_NAME;
            Connection conn = DriverManager.getConnection(fullUrl, USER, PASSWORD);

            // Bước 3: Tự động tạo bảng users (Sửa lại cấu hình cột cho khớp với UserService)
            // Lưu ý: Đổi user_id thành id để khớp với lệnh SELECT trong UserService
            String createTableUser = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "username VARCHAR(50) UNIQUE NOT NULL,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "email VARCHAR(100),"
                    + "role VARCHAR(20) DEFAULT 'USER',"
                    + "full_name VARCHAR(100),"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

            Statement tableStmt = conn.createStatement();
            tableStmt.execute(createTableUser);
            tableStmt.close();

            return conn;
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối MySQL tại DatabaseConnection: " + e.getMessage());
            throw e;
        }
    }
}
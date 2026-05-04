package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseConnection {
    // Dùng đúng port 3307 Jeff đã đổi trong XAMPP
    private static final String SERVER_URL = "jdbc:mysql://localhost:3307/";
    private static final String DB_NAME = "online_auction_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            // 1. Kết nối đến Server trước để tạo Database nếu chưa có
            Connection serverConn = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
            Statement stmt = serverConn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            serverConn.close();

            // 2. Kết nối chính thức vào Database
            String fullUrl = SERVER_URL + DB_NAME;
            Connection conn = DriverManager.getConnection(fullUrl, USER, PASSWORD);

            // 3. Tự động tạo bảng Users
            String createTableUser = "CREATE TABLE IF NOT EXISTS Users ("
                    + "user_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "username VARCHAR(50) UNIQUE NOT NULL,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "email VARCHAR(100) UNIQUE NOT NULL,"
                    + "role ENUM('ADMIN', 'USER') DEFAULT 'USER')";

            Statement tableStmt = conn.createStatement();
            tableStmt.execute(createTableUser);

            return conn;
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối MySQL: " + e.getMessage());
            throw e;
        }
    }
}
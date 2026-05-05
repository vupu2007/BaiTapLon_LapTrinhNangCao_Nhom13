package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    // Sửa về port 3306 theo đúng XAMPP hiện tại của bạn
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "online_auction_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            // 1. Đăng ký Driver (giúp Java nhận diện MySQL)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Kết nối tạm thời để đảm bảo DB tồn tại
            Connection serverConn = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
            Statement stmt = serverConn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            serverConn.close();

            // 3. Kết nối chính thức vào DB
            String fullUrl = SERVER_URL + DB_NAME;
            return DriverManager.getConnection(fullUrl, USER, PASSWORD);

        } catch (Exception e) {
            System.err.println("Lỗi kết nối Database: " + e.getMessage());
            return null;
        }
    }
}
package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/online_auction_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    // Singleton instance
    private static DatabaseConnection instance;
    private Connection connection;

    // Private constructor — bên ngoài không thể new DatabaseConnection()
    private DatabaseConnection() throws SQLException {
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Chỉ tạo instance nếu chưa có hoặc connection đã đóng
    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Các DAO gọi hàm này như cũ — không cần sửa gì ở nơi khác
    public static Connection getConnection() throws SQLException {
        return getInstance().connection;
    }
}
package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/online_auction_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    // Singleton — chỉ 1 instance của class này tồn tại
    private static DatabaseConnection instance;

    private DatabaseConnection() {}

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Mỗi lần gọi trả về connection MỚI — không cache connection
    // vì DAO dùng try-with-resources sẽ close() sau mỗi lần dùng
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
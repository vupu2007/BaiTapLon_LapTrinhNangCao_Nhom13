package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://sql7.freesqldatabase.com:3306/sql7826046";
    private static final String USER     = "sql7826046";
    private static final String PASSWORD = "UdU6CGM3MX";

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
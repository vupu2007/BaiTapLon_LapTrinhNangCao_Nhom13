package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCUtil {
    public static Connection getConnection() {
        Connection c = null;
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            String url = "jdbc:mysql://localhost:3306/auction_db";
            String user = "root";
            String password = "";
            c = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối Database: " + e.getMessage());
        }
        return c;
    }

    public static void closeConnection(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
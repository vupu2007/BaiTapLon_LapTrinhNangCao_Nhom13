package com.auction.server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String HOST = "bvfa8t3qtuhyhyzm2cns-mysql.services.clever-cloud.com";
    private static final String PORT = "3306";
    private static final String DB_NAME = "bvfa8t3qtuhyhyzm2cns";
    private static final String USER = "uvx5jzbzu6h6egtv";
    private static final String PASSWORD = "gxvNiCizwruEPSZnfwud";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Ho_Chi_Minh"
            + "&autoReconnect=true"
            + "&maxReconnects=5"
            + "&tcpKeepAlive=true";

    private static DatabaseConnection instance;

    private DatabaseConnection() {
        try {
            // Ép buộc nạp Driver để đảm bảo không bị mất luồng khi chạy đa luồng
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    /**
     * Thêm từ khóa 'synchronized' để bắt các luồng chạy ngầm của Server
     * phải xếp hàng lấy Connection một cách trật tự, tránh việc tạo connection song song
     * làm nghẽn băng thông mạng Internet kết nối tới Clever Cloud.
     */
    public static synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
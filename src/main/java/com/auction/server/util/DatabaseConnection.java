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

    // ĐÃ THÊM CẤU HÌNH TỰ ĐỘNG KẾT NỐI LẠI (AUTO RECONNECT) CHO CLOUD DATABASE
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Ho_Chi_Minh"
            + "&autoReconnect=true"        // Tự động kết nối lại nếu bị mất link
            + "&maxReconnects=5"           // Thử kết nối lại tối đa 5 lần nếu rớt mạng
            + "&tcpKeepAlive=true";        // Giữ đường truyền luôn "sống" liên tục

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
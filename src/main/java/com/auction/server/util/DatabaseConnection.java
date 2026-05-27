package com.auction.server.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        // 🛠️ ĐÃ SỬA: Thêm &allowPublicKeyRetrieval=true vào cuối chuỗi URL kết nối
        config.setJdbcUrl("jdbc:mysql://bvfa8t3qtuhyhyzm2cns-mysql.services.clever-cloud.com:3306/bvfa8t3qtuhyhyzm2cns?autoReconnect=true&tcpKeepAlive=true&useSSL=false&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true");
        config.setUsername("u7xxxxxxxxx"); // Điền user Clever Cloud của ông
        config.setPassword("pxxxxxxxxx"); // Điền pass Clever Cloud của ông

        // ⚡ BỘ THÔNG SỐ SIÊU TỐI ƯU CHO HIKARI:
        config.setMaximumPoolSize(10);        // Dự án BTL chỉ cần tối đa 10 kết nối dùng chung là đủ
        config.setMinimumIdle(3);             // Luôn giữ ít nhất 3 kết nối "sống" chờ sẵn
        config.setIdleTimeout(60000);         // 1 phút không dùng thì giải phóng bớt kết nối thừa
        config.setConnectionTimeout(5000);    // Quá 5 giây không kết nối được thì ngắt (đừng để treo luồng)
        config.setMaxLifetime(1800000);       // 30 phút tự zđộng làm mới kết nối để tránh bị Cloud kích

        // Tối ưu cache câu lệnh SQL cho MySQL Driver
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
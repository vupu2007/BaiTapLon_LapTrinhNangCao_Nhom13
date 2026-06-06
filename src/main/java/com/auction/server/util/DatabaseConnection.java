package com.auction.server.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/online_auction_db?autoReconnect=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh");
        config.setUsername("root");
        config.setPassword("");
        // ⚡ BỘ THÔNG SỐ SIÊU TỐI ƯU CHO HIKARI:
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);  // tăng lên 8s
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);

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
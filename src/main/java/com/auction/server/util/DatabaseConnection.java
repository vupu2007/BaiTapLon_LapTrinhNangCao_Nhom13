package com.auction.server.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://bvfa8t3qtuhyhyzm2cns-mysql.services.clever-cloud.com:3306/bvfa8t3qtuhyhyzm2cns?autoReconnect=true&tcpKeepAlive=true&useSSL=false&rewriteBatchedStatements=true");
        config.setUsername("uvx5jzbzu6h6egtv");
        config.setPassword("gxvNiCizwruEPSZnfwud");

        // 🎯 ĐÃ SỬA: Khống chế Pool tối đa là 3 để luôn dư kết nối cho hệ thống, tránh lỗi 'max_user_connections'
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);

        // Tối ưu cache câu lệnh SQL cho MySQL Driver
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        // Kiểm tra an toàn trước khi log thông tin Pool tránh lỗi NullPointerException lúc khởi tạo
        if (dataSource != null && dataSource.getHikariPoolMXBean() != null) {
            System.out.println("DEBUG Pool: active=" + dataSource.getHikariPoolMXBean().getActiveConnections()
                    + " idle=" + dataSource.getHikariPoolMXBean().getIdleConnections()
                    + " total=" + dataSource.getHikariPoolMXBean().getTotalConnections());
        }
        return dataSource.getConnection();
    }
}
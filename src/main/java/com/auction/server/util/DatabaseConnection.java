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
        config.setUsername("uvx5jzbzu6h6egtv"); // Điền user Clever Cloud của ông
        config.setPassword("gxvNiCizwruEPSZnfwud"); // Điền pass Clever Cloud của ông

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
        System.out.println("DEBUG Pool: active=" + dataSource.getHikariPoolMXBean().getActiveConnections()
                + " idle=" + dataSource.getHikariPoolMXBean().getIdleConnections()
                + " total=" + dataSource.getHikariPoolMXBean().getTotalConnections());
        return dataSource.getConnection();
    }
}
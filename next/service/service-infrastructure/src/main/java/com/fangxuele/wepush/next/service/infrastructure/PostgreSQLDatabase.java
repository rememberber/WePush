package com.fangxuele.wepush.next.service.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class PostgreSQLDatabase {
    private PostgreSQLDatabase() {
    }

    public static HikariDataSource create(String jdbcUrl, String username, String password,
                                          int maximumPoolSize) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL must use jdbc:postgresql://");
        }
        if (maximumPoolSize < 2 || maximumPoolSize > 200) {
            throw new IllegalArgumentException("PostgreSQL pool size must be between 2 and 200");
        }
        HikariConfig pool = new HikariConfig();
        pool.setJdbcUrl(jdbcUrl);
        pool.setUsername(username == null ? "" : username);
        pool.setPassword(password == null ? "" : password);
        pool.setDriverClassName("org.postgresql.Driver");
        pool.setPoolName("wepush-server-postgresql");
        pool.setMaximumPoolSize(maximumPoolSize);
        pool.setMinimumIdle(Math.min(2, maximumPoolSize));
        pool.setConnectionTimeout(10_000);
        pool.setValidationTimeout(5_000);
        pool.setKeepaliveTime(30_000);
        pool.setAutoCommit(true);
        pool.addDataSourceProperty("ApplicationName", "wepush-next-service");
        pool.addDataSourceProperty("tcpKeepAlive", "true");
        return new HikariDataSource(pool);
    }
}

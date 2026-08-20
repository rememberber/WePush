package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.App;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <pre>
 * Hikari连接池工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/20.
 */
public class HikariUtil {
    private volatile static HikariDataSource hikariDataSource;

    public static final String DB_TYPE_MYSQL = "MySQL";
    public static final String DB_TYPE_SQL_SERVER = "SQL Server";

    /**
     * 根据数据库类型构建JDBC URL
     */
    public static String buildJdbcUrl(String dbType, String dbUrl) {
        if (DB_TYPE_SQL_SERVER.equals(dbType)) {
            return "jdbc:sqlserver://" + dbUrl;
        }
        return "jdbc:mysql://" + dbUrl;
    }

    /**
     * 根据数据库类型设置数据源属性
     */
    public static void configureDataSource(HikariDataSource ds, String dbType, String dbUrl, String dbUser, String dbPassword) {
        ds.setJdbcUrl(buildJdbcUrl(dbType, dbUrl));
        ds.setUsername(dbUser);
        ds.setPassword(dbPassword);
        if (DB_TYPE_SQL_SERVER.equals(dbType)) {
            ds.addDataSourceProperty("encrypt", "false");
            ds.addDataSourceProperty("trustServerCertificate", "true");
        } else {
            ds.addDataSourceProperty("useSSL", "false");
            ds.addDataSourceProperty("autoReconnect", "true");
            ds.addDataSourceProperty("serverTimezone", "UTC");
            ds.addDataSourceProperty("characterEncoding", "utf-8");
            ds.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        }
    }

    /**
     * 获取数据源
     *
     * @return HikariDataSource
     */
    public static HikariDataSource getHikariDataSource() {
        if (hikariDataSource == null || hikariDataSource.isClosed()) {
            synchronized (HikariUtil.class) {
                if (hikariDataSource == null || hikariDataSource.isClosed()) {
                    String dbType = App.config.getDbType();
                    String mysqlUrl = App.config.getMysqlUrl();
                    String mysqlUser = App.config.getMysqlUser();
                    String mysqlPassword = App.config.getMysqlPassword();

                    hikariDataSource = new HikariDataSource();
                    configureDataSource(hikariDataSource, dbType, mysqlUrl, mysqlUser, mysqlPassword);
                }
            }
        }
        return hikariDataSource;
    }

    /**
     * 获取连接
     *
     * @return Connection
     * @throws SQLException SQLException
     */
    public static Connection getConnection() throws SQLException {
        return getHikariDataSource().getConnection();
    }

    /**
     * 执行查询
     *
     * @param sql sql
     * @return ResultSet
     */
    public static ResultSet executeQuery(String sql) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        return preparedStatement.executeQuery();
    }
}

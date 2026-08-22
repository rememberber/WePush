package com.fangxuele.wepush.next.service.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SQLiteDatabase {
    private SQLiteDatabase() {
    }

    public static HikariDataSource create(Path databasePath) {
        Path absolutePath = databasePath.toAbsolutePath().normalize();
        createParentDirectory(absolutePath);

        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.enforceForeignKeys(true);
        sqliteConfig.setBusyTimeout(5_000);
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        SQLiteDataSource sqlite = new SQLiteDataSource(sqliteConfig);
        sqlite.setUrl("jdbc:sqlite:" + absolutePath);

        HikariConfig pool = new HikariConfig();
        pool.setDataSource(sqlite);
        pool.setPoolName("wepush-standalone-sqlite");
        pool.setMaximumPoolSize(4);
        pool.setMinimumIdle(1);
        pool.setConnectionTimeout(5_000);
        pool.setAutoCommit(true);
        return new HikariDataSource(pool);
    }

    private static void createParentDirectory(Path databasePath) {
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot create database directory " + parent, exception);
        }
    }
}

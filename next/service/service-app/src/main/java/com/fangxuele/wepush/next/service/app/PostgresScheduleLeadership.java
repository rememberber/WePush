package com.fangxuele.wepush.next.service.app;

import javax.sql.DataSource;
import java.sql.Connection;

final class PostgresScheduleLeadership implements ScheduleLeadership {
    private static final long LOCK_ID = 0x57505553484cL;
    private final DataSource dataSource;
    private Connection connection;
    private boolean leader;

    PostgresScheduleLeadership(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public synchronized boolean isLeader() {
        try {
            if (leader && connection != null && connection.isValid(2)) return true;
            close();
            connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            try (var statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                statement.setLong(1, LOCK_ID);
                try (var result = statement.executeQuery()) {
                    leader = result.next() && result.getBoolean(1);
                }
            }
            if (!leader) close();
            return leader;
        } catch (Exception problem) {
            close();
            return false;
        }
    }

    @Override
    public synchronized void close() {
        leader = false;
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
            connection = null;
        }
    }
}

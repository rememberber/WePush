package com.fangxuele.wepush.next.service.app;

interface ScheduleLeadership extends AutoCloseable {
    boolean isLeader();

    @Override
    default void close() {
    }
}

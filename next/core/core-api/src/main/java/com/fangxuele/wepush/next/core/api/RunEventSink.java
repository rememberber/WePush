package com.fangxuele.wepush.next.core.api;

public interface RunEventSink extends AutoCloseable {
    void append(RunEvent event);

    void flush();

    @Override
    default void close() {
    }
}

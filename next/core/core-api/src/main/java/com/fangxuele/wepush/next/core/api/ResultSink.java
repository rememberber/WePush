package com.fangxuele.wepush.next.core.api;

import java.util.List;

public interface ResultSink extends AutoCloseable {
    void append(List<ItemResult> batch);

    void flush();

    @Override
    default void close() {
    }
}

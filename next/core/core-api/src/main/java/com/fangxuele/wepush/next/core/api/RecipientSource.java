package com.fangxuele.wepush.next.core.api;

import java.util.List;

public interface RecipientSource extends AutoCloseable {
    long totalCount();

    List<RecipientRecord> nextBatch(int maximumSize);

    @Override
    default void close() {
    }
}

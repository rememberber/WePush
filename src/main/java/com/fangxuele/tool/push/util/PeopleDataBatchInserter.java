package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.domain.TPeopleData;

import java.util.ArrayList;
import java.util.List;

/**
 * 人群数据批量插入，降低逐条 insert 的 SQLite 开销。
 */
public class PeopleDataBatchInserter implements AutoCloseable {

    public static final int DEFAULT_BATCH_SIZE = 200;

    private final TPeopleDataMapper mapper;
    private final int batchSize;
    private final List<TPeopleData> buffer;
    private int insertedCount;

    public PeopleDataBatchInserter(TPeopleDataMapper mapper) {
        this(mapper, DEFAULT_BATCH_SIZE);
    }

    public PeopleDataBatchInserter(TPeopleDataMapper mapper, int batchSize) {
        this.mapper = mapper;
        this.batchSize = Math.max(1, batchSize);
        this.buffer = new ArrayList<>(this.batchSize);
    }

    public void add(TPeopleData data) {
        buffer.add(data);
        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        mapper.insertBatch(buffer);
        insertedCount += buffer.size();
        buffer.clear();
    }

    public int getInsertedCount() {
        return insertedCount + buffer.size();
    }

    @Override
    public void close() {
        flush();
    }
}

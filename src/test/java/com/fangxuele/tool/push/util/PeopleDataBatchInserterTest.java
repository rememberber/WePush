package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.domain.TPeopleData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PeopleDataBatchInserter}.
 */
public class PeopleDataBatchInserterTest {

    /**
     * Simple stub mapper that records batch insert calls.
     */
    private static class StubMapper implements TPeopleDataMapper {
        final List<List<TPeopleData>> batches = new ArrayList<>();
        int totalInserted = 0;

        @Override
        public int insertBatch(List<TPeopleData> list) {
            batches.add(new ArrayList<>(list));
            totalInserted += list.size();
            return list.size();
        }

        // Unused mapper methods — provide no-op implementations
        @Override public int deleteByPrimaryKey(Integer id) { return 0; }
        @Override public int insert(TPeopleData record) { return 0; }
        @Override public int insertSelective(TPeopleData record) { return 0; }
        @Override public TPeopleData selectByPrimaryKey(Integer id) { return null; }
        @Override public int updateByPrimaryKeySelective(TPeopleData record) { return 0; }
        @Override public int updateByPrimaryKey(TPeopleData record) { return 0; }
        @Override public List<TPeopleData> selectByPeopleIdLimit20(Integer peopleId) { return null; }
        @Override public Long countByPeopleId(Integer peopleId) { return 0L; }
        @Override public int deleteByPeopleId(Integer peopleId) { return 0; }
        @Override public List<TPeopleData> selectByPeopleId(Integer peopleId) { return null; }
        @Override public List<TPeopleData> selectByPeopleIdAndKeyword(Integer peopleId, String keyWord) { return null; }
    }

    @Test
    public void testBatchFlush() {
        StubMapper mapper = new StubMapper();
        try (PeopleDataBatchInserter batcher = new PeopleDataBatchInserter(mapper, 3)) {
            for (int i = 0; i < 7; i++) {
                TPeopleData data = new TPeopleData();
                data.setPin("user" + i);
                batcher.add(data);
            }
            // 7 items with batch size 3: 2 full batches flushed (3+3), 1 remaining
            assertEquals(2, mapper.batches.size());
            assertEquals(3, mapper.batches.get(0).size());
            assertEquals(3, mapper.batches.get(1).size());
        }
        // After close, remaining 1 item should be flushed
        assertEquals(3, mapper.batches.size());
        assertEquals(1, mapper.batches.get(2).size());
        assertEquals(7, mapper.totalInserted);
    }

    @Test
    public void testEmptyBatcher() {
        StubMapper mapper = new StubMapper();
        try (PeopleDataBatchInserter batcher = new PeopleDataBatchInserter(mapper)) {
            // add nothing
        }
        assertEquals(0, mapper.totalInserted);
        assertTrue(mapper.batches.isEmpty());
    }

    @Test
    public void testGetInsertedCount() {
        StubMapper mapper = new StubMapper();
        PeopleDataBatchInserter batcher = new PeopleDataBatchInserter(mapper, 5);
        for (int i = 0; i < 3; i++) {
            TPeopleData data = new TPeopleData();
            batcher.add(data);
        }
        // 3 in buffer (not yet flushed), getInsertedCount includes buffered items
        assertEquals(3, batcher.getInsertedCount());

        for (int i = 0; i < 3; i++) {
            TPeopleData data = new TPeopleData();
            batcher.add(data);
        }
        // 5 flushed + 1 in buffer = 6
        assertEquals(6, batcher.getInsertedCount());
        batcher.close();
    }
}

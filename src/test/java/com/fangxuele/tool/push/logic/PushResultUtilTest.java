package com.fangxuele.tool.push.logic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PushResultUtil}.
 */
public class PushResultUtilTest {

    @Test
    public void testComputeUnsent_emptyInput() {
        List<String[]> result = PushResultUtil.computeUnsent(null, null, null, false);
        assertTrue(result.isEmpty());

        result = PushResultUtil.computeUnsent(new ArrayList<>(), null, null, false);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testComputeUnsent_noProcessed() {
        List<String[]> toSend = new ArrayList<>();
        toSend.add(new String[]{"a"});
        toSend.add(new String[]{"b"});

        List<String[]> result = PushResultUtil.computeUnsent(toSend, null, null, false);
        assertEquals(2, result.size());
    }

    @Test
    public void testComputeUnsent_identityBased() {
        String[] a = {"a"};
        String[] b = {"b"};
        String[] c = {"c"};
        List<String[]> toSend = new ArrayList<>(Arrays.asList(a, b, c));
        List<String[]> success = new ArrayList<>(Collections.singletonList(a));
        List<String[]> fail = new ArrayList<>(Collections.singletonList(c));

        List<String[]> result = PushResultUtil.computeUnsent(toSend, success, fail, false);
        assertEquals(1, result.size());
        assertSame(b, result.get(0));
    }

    @Test
    public void testComputeUnsent_httpSaveResult() {
        String[] a = {"a", "1"};
        String[] b = {"b", "2"};
        // In HTTP save result mode, success/fail lists have an extra trailing column
        String[] aWithBody = {"a", "1", "response_body"};

        List<String[]> toSend = new ArrayList<>(Arrays.asList(a, b));
        List<String[]> success = new ArrayList<>(Collections.singletonList(aWithBody));

        List<String[]> result = PushResultUtil.computeUnsent(toSend, success, null, true);
        assertEquals(1, result.size());
        assertArrayEquals(b, result.get(0));
    }

    @Test
    public void testComputeUnsent_allProcessed() {
        String[] a = {"a"};
        String[] b = {"b"};
        List<String[]> toSend = new ArrayList<>(Arrays.asList(a, b));
        List<String[]> success = new ArrayList<>(Arrays.asList(a, b));

        List<String[]> result = PushResultUtil.computeUnsent(toSend, success, null, false);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testComputeUnsent_largeDataPerformance() {
        // Verify that the set-based approach handles large data efficiently
        int size = 100_000;
        List<String[]> toSend = new ArrayList<>(size);
        List<String[]> success = new ArrayList<>(size / 2);
        for (int i = 0; i < size; i++) {
            String[] item = {String.valueOf(i)};
            toSend.add(item);
            if (i % 2 == 0) {
                success.add(item);
            }
        }

        long start = System.currentTimeMillis();
        List<String[]> result = PushResultUtil.computeUnsent(toSend, success, null, false);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(size / 2, result.size());
        // Should complete in well under 1 second with O(n) approach
        assertTrue("computeUnsent took too long: " + elapsed + "ms", elapsed < 5000);
    }
}

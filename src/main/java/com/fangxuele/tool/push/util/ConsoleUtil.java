package com.fangxuele.tool.push.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * <pre>
 * WePush控制台打印相关
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/12.
 */
@Slf4j
public class ConsoleUtil {

    private static final Log logger = LogFactory.get();

    private static final int FLUSH_EVERY = 32;

    /**
     * 按 writer 计数；访问时必须先锁 writer 再锁本 map，避免跨任务全局串行。
     */
    private static final Map<BufferedWriter, Integer> FLUSH_COUNTER = new IdentityHashMap<>();

    public static void pushLog(BufferedWriter logWriter, String content) {
        if (logWriter == null) {
            return;
        }
        try {
            synchronized (logWriter) {
                logWriter.write(SqliteUtil.nowDateForSqlite() + " " + content);
                logWriter.newLine();
                boolean shouldFlush;
                synchronized (FLUSH_COUNTER) {
                    int count = FLUSH_COUNTER.getOrDefault(logWriter, 0) + 1;
                    if (count >= FLUSH_EVERY) {
                        FLUSH_COUNTER.put(logWriter, 0);
                        shouldFlush = true;
                    } else {
                        FLUSH_COUNTER.put(logWriter, count);
                        shouldFlush = false;
                    }
                }
                if (shouldFlush) {
                    logWriter.flush();
                }
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    /**
     * 任务结束时刷出缓冲，避免日志丢失。
     */
    public static void flushLog(BufferedWriter logWriter) {
        if (logWriter == null) {
            return;
        }
        try {
            synchronized (logWriter) {
                logWriter.flush();
                synchronized (FLUSH_COUNTER) {
                    FLUSH_COUNTER.remove(logWriter);
                }
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }
}

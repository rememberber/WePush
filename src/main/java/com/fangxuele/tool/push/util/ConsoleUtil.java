package com.fangxuele.tool.push.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;

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

    public synchronized static void pushLog(BufferedWriter logWriter, String content) {
        try {
            logWriter.write(SqliteUtil.nowDateForSqlite() + " " + content);
            logWriter.newLine();
            logWriter.flush();
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }
}

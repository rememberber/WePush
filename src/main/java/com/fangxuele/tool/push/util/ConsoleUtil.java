package com.fangxuele.tool.push.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.form.PushForm;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * 输出到控制台和log
     *
     * @param log
     */
    public static void consoleWithLog(String log) {
        PushForm.pushForm.getPushConsoleTextArea().append(log + "\n");
        PushForm.pushForm.getPushConsoleTextArea().setCaretPosition(PushForm.pushForm.getPushConsoleTextArea().getText().length());
        logger.warn(log);
    }

    /**
     * 仅输出到控制台
     *
     * @param log
     */
    public static void consoleOnly(String log) {
        PushForm.pushForm.getPushConsoleTextArea().append(log + "\n");
        PushForm.pushForm.getPushConsoleTextArea().setCaretPosition(PushForm.pushForm.getPushConsoleTextArea().getText().length());
    }
}

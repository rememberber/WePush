package com.fangxuele.tool.push.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.ui.form.InfinityForm;
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
        PushForm.getInstance().getPushConsoleTextArea().append(log + "\n");
        PushForm.getInstance().getPushConsoleTextArea().setCaretPosition(PushForm.getInstance().getPushConsoleTextArea().getText().length());
        logger.warn(log);
    }

    /**
     * 输出到性能模式控制台和log
     *
     * @param log
     */
    public static void boostConsoleWithLog(String log) {
        BoostForm.getInstance().getConsoleTextArea().append(log + "\n");
        BoostForm.getInstance().getConsoleTextArea().setCaretPosition(BoostForm.getInstance().getConsoleTextArea().getText().length());
        logger.warn(log);
    }

    /**
     * 输出到变速模式控制台和log
     *
     * @param log
     */
    public static void infinityConsoleWithLog(String log) {
        InfinityForm.getInstance().getConsoleTextArea().append(log + "\n");
        InfinityForm.getInstance().getConsoleTextArea().setCaretPosition(InfinityForm.getInstance().getConsoleTextArea().getText().length());
        logger.warn(log);
    }

    /**
     * 仅输出到控制台
     *
     * @param log
     */
    public static void consoleOnly(String log) {
        PushForm.getInstance().getPushConsoleTextArea().append(log + "\n");
        PushForm.getInstance().getPushConsoleTextArea().setCaretPosition(PushForm.getInstance().getPushConsoleTextArea().getText().length());
    }

    /**
     * 仅输出到性能模式控制台
     *
     * @param log
     */
    public static void boostConsoleOnly(String log) {
        BoostForm.getInstance().getConsoleTextArea().append(log + "\n");
        BoostForm.getInstance().getConsoleTextArea().setCaretPosition(BoostForm.getInstance().getConsoleTextArea().getText().length());
    }

    /**
     * 仅输出到变速模式控制台
     *
     * @param log
     */
    public static void infinityConsoleOnly(String log) {
        InfinityForm.getInstance().getConsoleTextArea().append(log + "\n");
        InfinityForm.getInstance().getConsoleTextArea().setCaretPosition(InfinityForm.getInstance().getConsoleTextArea().getText().length());
    }
}

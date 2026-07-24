package com.fangxuele.tool.push.util;

import cn.hutool.core.thread.ThreadUtil;

import javax.swing.*;

/**
 * Swing EDT 调度工具：UI 更新回 EDT，重活离开 EDT。
 */
public final class UiThreadUtil {

    private UiThreadUtil() {
    }

    /**
     * 在 EDT 上执行；若当前已是 EDT 则同步执行。
     */
    public static void runOnUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * 在后台线程执行；若当前不在 EDT 则同步执行。
     */
    public static void runOffUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            ThreadUtil.execute(runnable);
        } else {
            runnable.run();
        }
    }
}

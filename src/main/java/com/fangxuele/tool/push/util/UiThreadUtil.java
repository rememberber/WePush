package com.fangxuele.tool.push.util;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Swing EDT 调度工具：UI 更新回 EDT，重活离开 EDT。
 */
public final class UiThreadUtil {

    /**
     * 导入进度刷新间隔（条），避免逐行 invokeLater 淹没 EDT。
     */
    public static final int IMPORT_PROGRESS_UI_INTERVAL = 100;

    /**
     * 单线程后台队列：避免启动时多个 Form 同时打共享 SqlSession。
     */
    private static final ExecutorService OFF_UI_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wepush-off-ui");
        t.setDaemon(true);
        return t;
    });

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
     * 从 EDT 提交时走单线程队列，保证顺序执行。
     */
    public static void runOffUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            OFF_UI_EXECUTOR.execute(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * 是否应刷新导入进度 UI（第 1 条或每 N 条）。
     */
    public static boolean shouldUpdateImportProgress(int currentImported) {
        return currentImported == 1 || currentImported % IMPORT_PROGRESS_UI_INTERVAL == 0;
    }
}

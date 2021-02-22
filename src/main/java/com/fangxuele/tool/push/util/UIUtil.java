package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.App;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * <pre>
 * UI自定义工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/6.
 */
@Slf4j
public class UIUtil {

    /**
     * 获取屏幕规格
     * author by darcula@com.bulenkov
     * see https://github.com/bulenkov/Darcula
     *
     * @return
     */
    public static float getScreenScale() {
        int dpi = 96;

        try {
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (HeadlessException var2) {
        }

        float scale = 1.0F;
        if (dpi < 120) {
            scale = 1.0F;
        } else if (dpi < 144) {
            scale = 1.25F;
        } else if (dpi < 168) {
            scale = 1.5F;
        } else if (dpi < 192) {
            scale = 1.75F;
        } else {
            scale = 2.0F;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        log.info("screen dpi:{},width:{},height:{}", dpi, screenSize.getWidth(), screenSize.getHeight());

        return scale;
    }

    /**
     * 是否暗黑主题
     *
     * @return
     */
    public static boolean isDarkLaf() {
        return "Darcula".equals(App.config.getTheme())
                || "Flat Dark".equals(App.config.getTheme())
                || "Flat Darcula(推荐)".equals(App.config.getTheme());
    }
}

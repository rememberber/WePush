package com.fangxuele.tool.push.util;

import org.junit.Test;

import java.awt.*;

/**
 * <pre>
 * 通用测试
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/17.
 */
public class CommonTest {

    @Test
    public void testGetSysFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        for (String font : fonts) {
            System.err.println(font);
        }
    }
}

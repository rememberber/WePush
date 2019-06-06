package com.fangxuele.tool.push.util;

import java.awt.*;

/**
 * <pre>
 * 组件工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/6.
 */
public class ComponentUtil {
    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    /**
     * 设置组件preferSize并定位于屏幕中央
     */
    public static void setPrefersizeAndLocateToCenter(Component component, int preferWidth, int preferHeight) {
        component.setBounds((screenSize.width - preferWidth) / 2, (screenSize.height - preferHeight) / 2,
                preferWidth, preferHeight);
        Dimension preferSize = new Dimension(preferWidth, preferHeight);
        component.setPreferredSize(preferSize);
    }

    /**
     * 设置组件preferSize并定位于屏幕中央(基于屏幕宽高的百分百)
     */
    public static void setPrefersizeAndLocateToCenter(Component component, double preferWidthPercent, double preferHeightPercent) {
        int preferWidth = (int) (screenSize.width * preferWidthPercent);
        int preferHeight = (int) (screenSize.height * preferHeightPercent);
        setPrefersizeAndLocateToCenter(component, preferWidth, preferHeight);
    }
}

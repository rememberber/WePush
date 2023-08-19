package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.App;

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

    private static Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(App.mainFrame.getGraphicsConfiguration());

    private static int screenWidth = screenSize.width - screenInsets.left - screenInsets.right;

    private static int screenHeight = screenSize.height - screenInsets.top - screenInsets.bottom;

    /**
     * 设置组件位于屏幕中央
     */
    public static void setLocateToCenter(Component component) {
        Dimension preferredSize = component.getPreferredSize();
        int preferWidth = (int) preferredSize.getWidth();
        int preferHeight = (int) preferredSize.getHeight();
        component.setBounds((screenWidth - preferWidth) / 2, (screenHeight - preferHeight) / 2,
                preferWidth, preferHeight);
    }

    /**
     * 设置组件preferSize并定位于屏幕中央
     */
    public static void setPreferSizeAndLocateToCenter(Component component, Integer preferWidth, Integer preferHeight) {
        Dimension initPreferredSize = component.getPreferredSize();
        if (preferWidth == null) {
            preferWidth = (int) initPreferredSize.getWidth();
        }
        if (preferHeight == null) {
            preferHeight = (int) initPreferredSize.getHeight();
        }

        component.setBounds((screenWidth - preferWidth) / 2, (screenHeight - preferHeight) / 2,
                preferWidth, preferHeight);
        Dimension preferSize = new Dimension(preferWidth, preferHeight);
        component.setPreferredSize(preferSize);
    }

    /**
     * 设置组件preferSize并定位于屏幕中央(基于屏幕宽高的百分百)
     */
    public static void setPreferSizeAndLocateToCenter(Component component, Double preferWidthPercent, Double preferHeightPercent) {
        Integer preferWidth = null;
        Integer preferHeight = null;

        if (preferWidthPercent != null) {
            preferWidth = (int) (screenWidth * preferWidthPercent);
        }
        if (preferHeightPercent != null) {
            preferHeight = (int) (screenHeight * preferHeightPercent);
        }

        setPreferSizeAndLocateToCenter(component, preferWidth, preferHeight);
    }
}

package com.fangxuele.tool.push.ui.frame;

import cn.hutool.core.thread.ThreadUtil;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.component.TopMenuBar;
import com.fangxuele.tool.push.ui.listener.*;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.formdev.flatlaf.util.SystemInfo;
import org.apache.commons.compress.utils.Lists;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * 主窗口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/2/14.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -332963894416012132L;

    public void init() {
        this.setName(UiConsts.APP_NAME);
        this.setTitle(UiConsts.APP_NAME);
        List<Image> images = Lists.newArrayList();
        images.add(UiConsts.IMAGE_LOGO_1024);
        images.add(UiConsts.IMAGE_LOGO_512);
        images.add(UiConsts.IMAGE_LOGO_256);
        images.add(UiConsts.IMAGE_LOGO_128);
        images.add(UiConsts.IMAGE_LOGO_64);
        images.add(UiConsts.IMAGE_LOGO_48);
        images.add(UiConsts.IMAGE_LOGO_32);
        images.add(UiConsts.IMAGE_LOGO_24);
        images.add(UiConsts.IMAGE_LOGO_16);
        this.setIconImages(images);

        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            this.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            this.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            this.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            this.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        TopMenuBar topMenuBar = TopMenuBar.getInstance();
        topMenuBar.init();
        setJMenuBar(topMenuBar);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.8, 0.88);
    }

    /**
     * 添加事件监听
     */
    public void addListeners() {
        ThreadUtil.execute(MessageTypeListener::addListeners);
        ThreadUtil.execute(HelpListener::addListeners);
        ThreadUtil.execute(AccountManageListener::addListeners);
        ThreadUtil.execute(AccountEditListener::addListeners);
        ThreadUtil.execute(MessageManageListener::addListeners);
        ThreadUtil.execute(MessageEditListener::addListeners);
        ThreadUtil.execute(PeopleManageListener::addListeners);
        ThreadUtil.execute(PeopleEditListener::addListeners);
        ThreadUtil.execute(TaskListener::addListeners);
        ThreadUtil.execute(TabListener::addListeners);
        ThreadUtil.execute(FrameListener::addListeners);
    }
}

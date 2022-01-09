package com.fangxuele.tool.push.ui.frame;

import cn.hutool.core.thread.ThreadUtil;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.listener.*;
import com.fangxuele.tool.push.util.ComponentUtil;
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
        // Mac系统Dock图标
//        if (SystemUtil.isMacOs()) {
//            Application application = Application.getApplication();
//            application.setDockIconImage(UiConsts.IMAGE_LOGO_1024);
//            if (!SystemUtil.isMacM1()) {
//                application.setEnabledAboutMenu(false);
//                application.setEnabledPreferencesMenu(false);
//            }
//        }

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.8, 0.88);
    }

    /**
     * 添加事件监听
     */
    public void addListeners() {
        ThreadUtil.execute(MessageTypeListener::addListeners);
        ThreadUtil.execute(AboutListener::addListeners);
        ThreadUtil.execute(HelpListener::addListeners);
        ThreadUtil.execute(PushHisListener::addListeners);
        ThreadUtil.execute(SettingListener::addListeners);
        ThreadUtil.execute(MessageEditListener::addListeners);
        ThreadUtil.execute(MessageManageListener::addListeners);
        ThreadUtil.execute(MemberListener::addListeners);
        ThreadUtil.execute(PushListener::addListeners);
        ThreadUtil.execute(InfinityListener::addListeners);
        ThreadUtil.execute(BoostListener::addListeners);
        ThreadUtil.execute(ScheduleListener::addListeners);
        ThreadUtil.execute(TabListener::addListeners);
        ThreadUtil.execute(FrameListener::addListeners);
    }
}

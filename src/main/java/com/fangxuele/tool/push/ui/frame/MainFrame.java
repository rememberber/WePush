package com.fangxuele.tool.push.ui.frame;

import com.apple.eawt.Application;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.ui.listener.BoostListener;
import com.fangxuele.tool.push.ui.listener.FrameListener;
import com.fangxuele.tool.push.ui.listener.HelpListener;
import com.fangxuele.tool.push.ui.listener.MemberListener;
import com.fangxuele.tool.push.ui.listener.MessageTypeListener;
import com.fangxuele.tool.push.ui.listener.MsgEditListener;
import com.fangxuele.tool.push.ui.listener.MsgManageListener;
import com.fangxuele.tool.push.ui.listener.PushHisListener;
import com.fangxuele.tool.push.ui.listener.PushListener;
import com.fangxuele.tool.push.ui.listener.ScheduleListener;
import com.fangxuele.tool.push.ui.listener.SettingListener;
import com.fangxuele.tool.push.ui.listener.TabListener;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.SystemUtil;
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
    public void init() {
        this.setName(UiConsts.APP_NAME);
        this.setTitle(UiConsts.APP_NAME);
        List<Image> images = Lists.newArrayList();
        images.add(UiConsts.IMAGE_ICON_LG);
        images.add(UiConsts.IMAGE_ICON_MD);
        images.add(UiConsts.IMAGE_ICON_SM);
        images.add(UiConsts.IMAGE_ICON_XS);
        this.setIconImages(images);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_ICON_LG);
        }

        ComponentUtil.setPrefersizeAndLocateToCenter(this, 0.8, 0.88);
    }

    /**
     * 添加事件监听
     */
    public void addListeners() {
        MessageTypeListener.addListeners();
        AboutListener.addListeners();
        HelpListener.addListeners();
        PushHisListener.addListeners();
        SettingListener.addListeners();
        MsgEditListener.addListeners();
        MsgManageListener.addListeners();
        MemberListener.addListeners();
        PushListener.addListeners();
        BoostListener.addListeners();
        ScheduleListener.addListeners();
        TabListener.addListeners();
        FrameListener.addListeners();
    }
}

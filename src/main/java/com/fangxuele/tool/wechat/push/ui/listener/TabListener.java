package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * tab事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/21.
 */
public class TabListener {

    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        // 暂时停止使用，仅留作demo，日后需要时再使用
        MainWindow.mainWindow.getTabbedPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int index = MainWindow.mainWindow.getTabbedPane().getSelectedIndex();
                switch (index) {
                    case 6:
                        break;
                    default:
                        break;
                }
            }
        });
    }
}

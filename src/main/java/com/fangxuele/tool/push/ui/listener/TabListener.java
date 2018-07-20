package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * tab事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/21.
 */
public class TabListener {

    private static final Log logger = LogFactory.get();

    private static boolean warnFlag = true;

    public static void addListeners() {
        // 暂时停止使用，仅留作demo，日后需要时再使用
        MainWindow.mainWindow.getTabbedPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int index = MainWindow.mainWindow.getTabbedPane().getSelectedIndex();
                switch (index) {
                    case 6:
                        Init.initPushHisTab();
                        break;
                    case 3:
                        if (warnFlag) {
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "\n请确认您了解所要发送消息类型的使用频率、使用规范和限制规则，\n" +
                                            "以免账号相关功能被封禁等给您带来麻烦", "提示",
                                    JOptionPane.INFORMATION_MESSAGE);
                            warnFlag = false;
                        }
                        break;
                    case 4:
                        MainWindow.mainWindow.setPushMsgName(MainWindow.mainWindow.getMsgNameField().getText());

                        if (PushData.allUser != null && PushData.allUser.size() > 0) {
                            PushListener.refreshPushInfo();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }
}

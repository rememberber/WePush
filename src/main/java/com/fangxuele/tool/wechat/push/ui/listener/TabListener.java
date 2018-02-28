package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.logic.PushData;
import com.fangxuele.tool.wechat.push.ui.Init;
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
                        Init.initPushHisTab();
                        break;
                    case 4:
                        MainWindow.mainWindow.setPushMsgName(MainWindow.mainWindow.getMsgNameField().getText());

                        if (PushData.allUser != null && PushData.allUser.size() > 0) {
                            // 页大小
                            int pageSize = Integer.parseInt(MainWindow.mainWindow.getPushPageSizeTextField().getText());
                            // 总记录数
                            long totalCount = PushData.allUser.size();
                            MainWindow.mainWindow.getPushTotalCountLabel().setText("总用户数：" + totalCount);
                            MainWindow.mainWindow.getPushTotalProgressBar().setMaximum((int) totalCount);
                            // 总页数
                            int totalPage = Long.valueOf((totalCount + pageSize - 1) / pageSize).intValue();
                            MainWindow.mainWindow.getPushTotalPageLabel().setText("总页数：" + totalPage);
                            // 每个线程分配多少页
                            int pagePerThread = Integer.parseInt(MainWindow.mainWindow.getPushPagePerThreadTextField().getText());
                            // 需要多少个线程
                            int threadCount = (totalPage + pagePerThread - 1) / pagePerThread;
                            MainWindow.mainWindow.getPushTotalThreadLabel().setText("需要线程宝宝个数：" + threadCount);
                        }

                    default:
                        break;
                }
            }
        });
    }
}

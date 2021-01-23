package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.ui.form.InfinityForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <pre>
 * tab事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/21.
 */
public class TabListener {

    private static boolean warnFlag = true;

    public static void addListeners() {
        MainWindow.getInstance().getTabbedPane().addChangeListener(new ChangeListener() {
            /**
             * Invoked when the target of the listener has changed its state.
             *
             * @param e a ChangeEvent object
             */
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = MainWindow.getInstance().getTabbedPane().getSelectedIndex();
                int msgType = App.config.getMsgType();
                switch (index) {
                    case 0:
                        AboutForm.init();
                        break;
                    case 3:
                        if (warnFlag && msgType != MessageTypeEnum.EMAIL_CODE && msgType != MessageTypeEnum.HTTP_CODE) {
                            JOptionPane.showMessageDialog(MainWindow.getInstance().getSettingPanel(), "\n请确认您了解所要发送消息类型的使用频率、使用规范和限制规则，\n" +
                                            "以免账号相关功能被封禁等给您带来麻烦\n", "提示",
                                    JOptionPane.INFORMATION_MESSAGE);
                            warnFlag = false;
                        }
                        break;
                    case 4:
                        PushForm.getInstance().getPushMsgName().setText(MessageEditForm.getInstance().getMsgNameField().getText());
                        PushListener.refreshPushInfo();
                        break;
                    case 5:
                        InfinityForm.getInstance().getPushMsgName().setText(MessageEditForm.getInstance().getMsgNameField().getText());
                        InfinityListener.refreshPushInfo();
                        break;
                    case 6:
                        BoostForm.getInstance().getMsgNameLabel().setText(MessageEditForm.getInstance().getMsgNameField().getText());
                        BoostListener.refreshPushInfo();
                        break;
                    default:
                        break;
                }
            }
        });
    }
}

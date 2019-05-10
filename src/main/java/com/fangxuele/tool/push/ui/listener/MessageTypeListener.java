package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MessageTypeForm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.fangxuele.tool.push.ui.form.MessageTypeForm.messageTypeForm;

/**
 * <pre>
 * 消息类型form事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/10.
 */
public class MessageTypeListener {

    public static void addListeners() {
        messageTypeForm.getMpTemplateRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(1);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getMaTemplateRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(2);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getKefuRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(3);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getKefuPriorityRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(4);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getAliYunRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(5);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getAliTemplateRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(6);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getTxYunRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(7);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getYunPianRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(8);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getUpYunRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(9);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getHwYunRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(10);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
        messageTypeForm.getEMailRadioButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTypeForm.clearAllSelected();
                Init.config.setMsgType(11);
                Init.config.save();
                MessageTypeForm.init();
            }
        });
    }
}

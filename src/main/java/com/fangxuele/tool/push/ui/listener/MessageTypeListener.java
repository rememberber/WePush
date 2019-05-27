package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MessageTypeForm;

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
        messageTypeForm.getMpTemplateRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.MP_TEMPLATE_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getMaTemplateRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.MA_TEMPLATE_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getKefuRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.KEFU_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getKefuPriorityRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.KEFU_PRIORITY_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getAliYunRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.ALI_YUN_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getAliTemplateRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.ALI_TEMPLATE_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getTxYunRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.TX_YUN_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getYunPianRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.YUN_PIAN_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getUpYunRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.UP_YUN_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getHwYunRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.HW_YUN_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getEMailRadioButton().addActionListener(e -> {
            App.config.setMsgType(MessageTypeEnum.EMAIL_CODE);
            App.config.save();
            MessageTypeForm.init();
        });
    }
}

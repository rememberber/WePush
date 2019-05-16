package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.Init;
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
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.MP_TEMPLATE_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getMaTemplateRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.MA_TEMPLATE_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getKefuRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.KEFU_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getKefuPriorityRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.KEFU_PRIORITY_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getAliYunRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.ALI_YUN_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getAliTemplateRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.ALI_TEMPLATE_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getTxYunRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.TX_YUN_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getYunPianRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.YUN_PIAN_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getUpYunRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.UP_YUN_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getHwYunRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.HW_YUN_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
        messageTypeForm.getEMailRadioButton().addActionListener(e -> {
            MessageTypeForm.clearAllSelected();
            Init.config.setMsgType(MessageTypeEnum.EMAIL_CODE);
            Init.config.save();
            MessageTypeForm.init();
        });
    }
}

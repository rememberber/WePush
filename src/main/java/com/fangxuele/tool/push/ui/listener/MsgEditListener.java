package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.msg.AliTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.AliYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.KefuMsgForm;
import com.fangxuele.tool.push.ui.form.msg.KefuPriorityMsgForm;
import com.fangxuele.tool.push.ui.form.msg.MaTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.MpTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.TxYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.YunpianMsgForm;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 编辑消息tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/18.
 */
public class MsgEditListener {
    private static final Log logger = LogFactory.get();

    private static JSplitPane messagePanel = MainWindow.mainWindow.getMessagePanel();

    public static void addListeners() {
        // 保存按钮事件
        MessageEditForm.messageEditForm.getMsgSaveButton().addActionListener(e -> {
            String msgName = MessageEditForm.messageEditForm.getMsgNameField().getText();
            if (StringUtils.isBlank(msgName)) {
                JOptionPane.showMessageDialog(messagePanel, "请填写消息名称！\n\n", "失败",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int msgType = App.config.getMsgType();

            try {
                switch (msgType) {
                    case MessageTypeEnum.KEFU_CODE:
                        KefuMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.KEFU_PRIORITY_CODE:
                        KefuPriorityMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.MA_TEMPLATE_CODE:
                        MaTemplateMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.MP_TEMPLATE_CODE:
                        MpTemplateMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.ALI_TEMPLATE_CODE:
                        AliTemplateMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.ALI_YUN_CODE:
                        AliYunMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.TX_YUN_CODE:
                        TxYunMsgForm.save(msgName);
                        break;
                    case MessageTypeEnum.YUN_PIAN_CODE:
                        YunpianMsgForm.save(msgName);
                        break;
                    default:
                }

                App.config.setPreviewUser(MessageEditForm.messageEditForm.getPreviewUserField().getText());
                App.config.save();
                MessageManageForm.init();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(messagePanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }

        });

        // 预览按钮事件
        MessageEditForm.messageEditForm.getPreviewMsgButton().addActionListener(e -> {
            try {
                if ("".equals(MessageEditForm.messageEditForm.getPreviewUserField().getText().trim())) {
                    JOptionPane.showMessageDialog(messagePanel, "预览用户不能为空！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (App.config.getMsgType() == MessageTypeEnum.MA_TEMPLATE_CODE
                        && MessageEditForm.messageEditForm.getPreviewUserField().getText().split(";")[0].length() < 2) {
                    JOptionPane.showMessageDialog(messagePanel, "小程序模板消息预览时，“预览用户openid”输入框里填写openid|formId，\n" +
                                    "示例格式：\n" +
                                    "opd-aswadfasdfasdfasdf|fi291834543", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (PushManage.preview()) {
                    JOptionPane.showMessageDialog(messagePanel, "发送预览消息成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(messagePanel, "发送预览消息失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        MessageEditForm.messageEditForm.getPreviewUserHelpLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                int msgType = App.config.getMsgType();
                String fillParaName;
                String paraDemo;
                if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE || msgType == MessageTypeEnum.KEFU_PRIORITY_CODE
                        || msgType == MessageTypeEnum.KEFU_CODE) {
                    fillParaName = "openId";
                    paraDemo = "ox_kxwS_gGt63adS-zemlETtuvw1;ox_kxwS_gGt63adS-zemlETtuvw2";
                } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                    fillParaName = "openId|formId";
                    paraDemo = "opd-aswadfasdfasdfasdf|fi291834543;opd-aswadfasdfasdfasdf2|fi2918345432";
                } else {
                    fillParaName = "手机号";
                    paraDemo = "13910733521;13910733522";
                }
                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>如何填写？</h1>");
                tipsBuilder.append("<h2>此处填写预览消息用户的").append(fillParaName).append("</h2>");
                tipsBuilder.append("<p>如有多个，请以半角分号分隔</p>");
                tipsBuilder.append("<p>示例：</p>");
                tipsBuilder.append("<p>").append(paraDemo).append("</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                MessageEditForm.messageEditForm.getPreviewUserHelpLabel().setCursor(new Cursor(Cursor.HAND_CURSOR));
                MessageEditForm.messageEditForm.getPreviewUserHelpLabel().setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                MessageEditForm.messageEditForm.getPreviewUserHelpLabel().setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }
}
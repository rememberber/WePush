package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/** 运营商协议短信内容编辑表单。 */
public class CarrierSmsMsgForm implements IMsgForm {
    private static final TMsgMapper MSG_MAPPER = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);
    private static CarrierSmsMsgForm instance;

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JTextArea contentTextArea = new JTextArea();

    private CarrierSmsMsgForm() {
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "短信内容编辑（支持 Velocity 变量）", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, mainPanel.getFont().deriveFont(Font.BOLD)));
        contentTextArea.setLineWrap(true);
        contentTextArea.setWrapStyleWord(true);
        contentTextArea.setRows(12);
        mainPanel.add(new JScrollPane(contentTextArea), BorderLayout.CENTER);
    }

    public static CarrierSmsMsgForm getInstance() {
        if (instance == null) {
            instance = new CarrierSmsMsgForm();
        }
        return instance;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JTextArea getContentTextArea() {
        return contentTextArea;
    }

    @Override
    public void init(Integer msgId) {
        clearAllField();
        TMsg message = MSG_MAPPER.selectByPrimaryKey(msgId);
        if (message == null) {
            return;
        }
        TMsgSms sms = JSONUtil.toBean(message.getContent(), TMsgSms.class);
        contentTextArea.setText(sms == null ? "" : sms.getContent());
        MessageEditForm editForm = MessageEditForm.getInstance();
        editForm.getMsgNameField().setText(message.getMsgName());
        editForm.getPreviewUserField().setText(message.getPreviewUser());
    }

    @Override
    public void save(Integer accountId, String msgName) {
        TMsg existing = MSG_MAPPER.selectByUnique(MessageTypeEnum.CARRIER_SMS_CODE, accountId, msgName);
        if (existing != null) {
            int cover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(),
                    "已经存在同名的历史消息，\n是否覆盖？", "确认", JOptionPane.YES_NO_OPTION);
            if (cover != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String now = SqliteUtil.nowDateForSqlite();
        TMsg message = new TMsg();
        message.setMsgType(MessageTypeEnum.CARRIER_SMS_CODE);
        message.setAccountId(accountId);
        message.setMsgName(msgName);
        message.setModifiedTime(now);
        message.setPreviewUser(MessageEditForm.getInstance().getPreviewUserField().getText());
        TMsgSms sms = new TMsgSms();
        sms.setContent(contentTextArea.getText());
        message.setContent(JSONUtil.toJsonStr(sms));
        if (existing == null) {
            message.setCreateTime(now);
            MSG_MAPPER.insertSelective(message);
        } else {
            message.setId(existing.getId());
            MSG_MAPPER.updateByPrimaryKeySelective(message);
        }
        JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void clearAllField() {
        contentTextArea.setText("");
    }
}

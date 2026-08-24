package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgFeishu;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.FeishuBotSupport;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * 飞书群自定义机器人消息编辑界面。
 */
public class FeishuMsgForm implements IMsgForm {
    private final JPanel feishuMsgPanel = new JPanel(new GridBagLayout());
    private final JComboBox<String> msgTypeComboBox = new JComboBox<>(new String[]{
            FeishuBotSupport.TYPE_TEXT,
            FeishuBotSupport.TYPE_POST,
            FeishuBotSupport.TYPE_CARD,
            FeishuBotSupport.TYPE_RAW_JSON
    });
    private final JLabel mentionLabel = new JLabel("提醒方式");
    private final JComboBox<String> mentionTypeComboBox = new JComboBox<>(new String[]{
            FeishuBotSupport.MENTION_NONE,
            FeishuBotSupport.MENTION_FIRST_COLUMN,
            FeishuBotSupport.MENTION_ALL
    });
    private final JLabel titleLabel = new JLabel("标题 *");
    private final JTextField titleTextField = new JTextField();
    private final JLabel contentLabel = new JLabel("内容 *");
    private final JTextArea contentTextArea = new JTextArea(16, 60);
    private final JLabel contentHintLabel = new JLabel();

    private static final TMsgMapper MSG_MAPPER = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);
    private static FeishuMsgForm instance;

    private FeishuMsgForm() {
        GridBagConstraints label = constraints(0, 0, 0, 0, GridBagConstraints.NONE);
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(5, 5, 5, 12);
        GridBagConstraints field = constraints(1, 0, 1, 0, GridBagConstraints.HORIZONTAL);
        field.insets = new Insets(5, 0, 5, 5);

        feishuMsgPanel.add(new JLabel("消息类型 *"), label);
        feishuMsgPanel.add(msgTypeComboBox, field);

        label.gridy = 1;
        field.gridy = 1;
        feishuMsgPanel.add(mentionLabel, label);
        feishuMsgPanel.add(mentionTypeComboBox, field);

        label.gridy = 2;
        field.gridy = 2;
        feishuMsgPanel.add(titleLabel, label);
        feishuMsgPanel.add(titleTextField, field);

        label.gridy = 3;
        feishuMsgPanel.add(contentLabel, label);
        contentTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, contentTextArea.getFont().getSize()));
        contentTextArea.setLineWrap(false);
        JScrollPane scrollPane = new JScrollPane(contentTextArea);
        GridBagConstraints content = constraints(1, 3, 1, 1, GridBagConstraints.BOTH);
        content.insets = new Insets(5, 0, 5, 5);
        feishuMsgPanel.add(scrollPane, content);

        GridBagConstraints hint = constraints(0, 4, 1, 0, GridBagConstraints.HORIZONTAL);
        hint.gridwidth = 2;
        hint.insets = new Insets(5, 5, 5, 5);
        feishuMsgPanel.add(contentHintLabel, hint);

        msgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchMessageType(e.getItem().toString());
            }
        });
        switchMessageType(FeishuBotSupport.TYPE_TEXT);
    }

    public static FeishuMsgForm getInstance() {
        if (instance == null) {
            instance = new FeishuMsgForm();
        }
        return instance;
    }

    public JPanel getFeishuMsgPanel() {
        return feishuMsgPanel;
    }

    @Override
    public void init(Integer msgId) {
        clearAllField();
        if (msgId == null) {
            return;
        }
        TMsg tMsg = MSG_MAPPER.selectByPrimaryKey(msgId);
        if (tMsg == null) {
            return;
        }
        TMsgFeishu message = JSONUtil.toBean(tMsg.getContent(), TMsgFeishu.class);
        if (message != null) {
            msgTypeComboBox.setSelectedItem(message.getFeishuMsgType());
            titleTextField.setText(StringUtils.defaultString(message.getTitle()));
            contentTextArea.setText(StringUtils.defaultString(message.getContent()));
            mentionTypeComboBox.setSelectedItem(StringUtils.defaultIfBlank(
                    message.getMentionType(), FeishuBotSupport.MENTION_NONE));
            switchMessageType(message.getFeishuMsgType());
        }
        MessageEditForm editForm = MessageEditForm.getInstance();
        editForm.getMsgNameField().setText(tMsg.getMsgName());
        editForm.getPreviewUserField().setText(tMsg.getPreviewUser());
    }

    @Override
    public void save(Integer accountId, String msgName) {
        String messageType = Objects.requireNonNull(msgTypeComboBox.getSelectedItem()).toString();
        String title = titleTextField.getText();
        String content = contentTextArea.getText();
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (FeishuBotSupport.TYPE_POST.equals(messageType) && StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("富文本消息标题不能为空");
        }
        // 不含模板变量时提前检查 JSON；含变量的消息在渲染后由发送器检查。
        if ((FeishuBotSupport.TYPE_CARD.equals(messageType)
                || FeishuBotSupport.TYPE_RAW_JSON.equals(messageType)) && !content.contains("$")) {
            FeishuBotSupport.buildPayload(messageType, title, content, "", "");
        }

        TMsg existing = MSG_MAPPER.selectByUnique(MessageTypeEnum.FEISHU_CODE, accountId, msgName);
        int cover = JOptionPane.NO_OPTION;
        if (existing != null) {
            cover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(),
                    "已经存在同名的历史消息，\n是否覆盖？", "确认", JOptionPane.YES_NO_OPTION);
        }
        if (existing != null && cover != JOptionPane.YES_OPTION) {
            return;
        }

        TMsgFeishu message = new TMsgFeishu();
        message.setFeishuMsgType(messageType);
        message.setTitle(title);
        message.setContent(content);
        message.setMentionType(Objects.requireNonNull(mentionTypeComboBox.getSelectedItem()).toString());

        String now = SqliteUtil.nowDateForSqlite();
        TMsg tMsg = new TMsg();
        tMsg.setMsgType(MessageTypeEnum.FEISHU_CODE);
        tMsg.setAccountId(accountId);
        tMsg.setMsgName(msgName);
        tMsg.setContent(JSONUtil.toJsonStr(message));
        tMsg.setPreviewUser(MessageEditForm.getInstance().getPreviewUserField().getText());
        tMsg.setModifiedTime(now);
        if (existing != null) {
            tMsg.setId(existing.getId());
            MSG_MAPPER.updateByPrimaryKeySelective(tMsg);
        } else {
            tMsg.setCreateTime(now);
            MSG_MAPPER.insertSelective(tMsg);
        }
        JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void clearAllField() {
        msgTypeComboBox.setSelectedItem(FeishuBotSupport.TYPE_TEXT);
        titleTextField.setText("");
        contentTextArea.setText("");
        mentionTypeComboBox.setSelectedItem(FeishuBotSupport.MENTION_NONE);
        switchMessageType(FeishuBotSupport.TYPE_TEXT);
    }

    private void switchMessageType(String messageType) {
        boolean post = FeishuBotSupport.TYPE_POST.equals(messageType);
        boolean supportsMention = FeishuBotSupport.TYPE_TEXT.equals(messageType) || post;
        mentionLabel.setVisible(supportsMention);
        mentionTypeComboBox.setVisible(supportsMention);
        titleLabel.setVisible(post);
        titleTextField.setVisible(post);
        if (FeishuBotSupport.TYPE_CARD.equals(messageType)) {
            contentLabel.setText("卡片 JSON *");
            contentHintLabel.setText("<html>填写飞书 card 对象，例如 {\"header\":{...},\"elements\":[...]}。可使用 $var0、$DATE 等变量。</html>");
        } else if (FeishuBotSupport.TYPE_RAW_JSON.equals(messageType)) {
            contentLabel.setText("完整请求 JSON *");
            contentHintLabel.setText("<html>填写完整飞书机器人请求体，可用于图片、分享群名片或未来消息类型；关键词和 @ 需在 JSON 中自行配置。</html>");
        } else if (post) {
            contentLabel.setText("正文 *");
            contentHintLabel.setText("<html>正文按富文本 text 标签发送；提醒方式可选择不 @、@ 数据第 1 列 open_id 或 @所有人。</html>");
        } else {
            contentLabel.setText("内容 *");
            contentHintLabel.setText("<html>支持 $var0、$var1、$DATE、$TIME 等变量；提醒方式可选择不 @、@ 数据第 1 列 open_id 或 @所有人。</html>");
        }
        feishuMsgPanel.revalidate();
        feishuMsgPanel.repaint();
    }

    private static GridBagConstraints constraints(int x, int y, double weightX, double weightY, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightX;
        constraints.weighty = weightY;
        constraints.fill = fill;
        return constraints;
    }
}

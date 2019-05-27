package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TMsgMaTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgMpTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgSmsMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.domain.TMsgMaTemplate;
import com.fangxuele.tool.push.domain.TMsgMpTemplate;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.domain.TTemplateData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <pre>
 * 编辑消息tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/18.
 */
public class MsgListener {
    private static final Log logger = LogFactory.get();

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private static TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    private static JTable msgHistable = MessageManageForm.messageManageForm.getMsgHistable();
    private static JSplitPane messagePanel = MainWindow.mainWindow.getMessagePanel();

    public static void addListeners() {

        // 点击左侧表格事件
        msgHistable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    PushHisForm.pushHisForm.getPushHisTextArea().setText("");

                    int selectedRow = msgHistable.getSelectedRow();
                    String selectedMsgName = msgHistable
                            .getValueAt(selectedRow, 0).toString();

                    MessageEditForm.init(selectedMsgName);
                });
                super.mouseClicked(e);
            }
        });

        // 历史消息管理-删除
        MessageManageForm.messageManageForm.getMsgHisTableDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = msgHistable.getSelectedRows();

                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(messagePanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(messagePanel, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) msgHistable
                                .getModel();
                        int msgType = App.config.getMsgType();

                        for (int i = selectedRows.length; i > 0; i--) {
                            int selectedRow = msgHistable.getSelectedRow();
                            String msgName = (String) tableModel.getValueAt(selectedRow, 0);
                            if (msgType == MessageTypeEnum.KEFU_CODE) {
                                msgKefuMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
                                msgKefuPriorityMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                                msgMaTemplateMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
                                msgMpTemplateMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else {
                                msgSmsMapper.deleteByMsgTypeAndName(msgType, msgName);
                            }

                            tableModel.removeRow(selectedRow);
                        }
                        MessageEditForm.init(null);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(messagePanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 客服消息类型切换事件
        MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().addItemListener(e -> MessageEditForm.switchKefuMsgType(e.getItem().toString()));

        // 模板数据-添加 按钮事件
        MessageEditForm.messageEditForm.getTemplateMsgDataAddButton().addActionListener(e -> {
            String[] data = new String[3];
            data[0] = MessageEditForm.messageEditForm.getTemplateDataNameTextField().getText();
            data[1] = MessageEditForm.messageEditForm.getTemplateDataValueTextField().getText();
            data[2] = MessageEditForm.messageEditForm.getTemplateDataColorTextField().getText();

            if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MessageEditForm.initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(messagePanel, "Name或value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(messagePanel, "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                if (StringUtils.isEmpty(data[2])) {
                    data[2] = "#000000";
                } else if (!data[2].startsWith("#")) {
                    data[2] = "#" + data[2];
                }
                tableModel.addRow(data);
            }
        });

        // 保存按钮事件
        MessageEditForm.messageEditForm.getMsgSaveButton().addActionListener(e -> {
            String msgName = MessageEditForm.messageEditForm.getMsgNameField().getText();
            if (StringUtils.isBlank(msgName)) {
                JOptionPane.showMessageDialog(messagePanel, "请填写推送任务名称！\n\n", "失败",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int msgType = App.config.getMsgType();
            int msgId = 0;

            boolean existSameMsg = false;
            if (msgType == MessageTypeEnum.KEFU_CODE) {
                List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(msgType, msgName);
                if (tMsgKefuList.size() > 0) {
                    existSameMsg = true;
                    msgId = tMsgKefuList.get(0).getId();
                }
            } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
                List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndMsgName(msgType, msgName);
                if (tMsgKefuPriorityList.size() > 0) {
                    existSameMsg = true;
                    msgId = tMsgKefuPriorityList.get(0).getId();
                }
            } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                List<TMsgMaTemplate> tMsgMaTemplateList = msgMaTemplateMapper.selectByMsgTypeAndMsgName(msgType, msgName);
                if (tMsgMaTemplateList.size() > 0) {
                    existSameMsg = true;
                    msgId = tMsgMaTemplateList.get(0).getId();
                }
            } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
                List<TMsgMpTemplate> tMsgMpTemplateList = msgMpTemplateMapper.selectByMsgTypeAndMsgName(msgType, msgName);
                if (tMsgMpTemplateList.size() > 0) {
                    existSameMsg = true;
                    msgId = tMsgMpTemplateList.get(0).getId();
                }
            } else {
                List<TMsgSms> tMsgSmsList = msgSmsMapper.selectByMsgTypeAndMsgName(msgType, msgName);
                if (tMsgSmsList.size() > 0) {
                    existSameMsg = true;
                    msgId = tMsgSmsList.get(0).getId();
                }
            }

            int isCover = JOptionPane.NO_OPTION;
            if (existSameMsg) {
                // 如果存在，是否覆盖
                isCover = JOptionPane.showConfirmDialog(messagePanel, "已经存在同名的历史消息，\n是否覆盖？", "确认",
                        JOptionPane.YES_NO_OPTION);
            }

            try {
                if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
                    String templateId = MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText();
                    String templateUrl = MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().getText();
                    String kefuMsgType = Objects.requireNonNull(MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
                    String kefuMsgTitle = MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().getText();
                    String kefuPicUrl = MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().getText();
                    String kefuDesc = MessageEditForm.messageEditForm.getMsgKefuDescTextField().getText();
                    String kefuUrl = MessageEditForm.messageEditForm.getMsgKefuUrlTextField().getText();
                    String templateMiniAppid = MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().getText();
                    String templateMiniPagePath = MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().getText();
                    String templateKeyWord = MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().getText();
                    String yunpianMsgContent = MessageEditForm.messageEditForm.getMsgYunpianMsgContentTextField().getText();

                    String now = SqliteUtil.nowDateForSqlite();

                    if (msgType == MessageTypeEnum.KEFU_CODE) {
                        TMsgKefu tMsgKefu = new TMsgKefu();
                        tMsgKefu.setMsgType(msgType);
                        tMsgKefu.setMsgName(msgName);
                        tMsgKefu.setKefuMsgType(kefuMsgType);
                        tMsgKefu.setContent(kefuMsgTitle);
                        tMsgKefu.setTitle(kefuMsgTitle);
                        tMsgKefu.setImgUrl(kefuPicUrl);
                        tMsgKefu.setDescribe(kefuDesc);
                        tMsgKefu.setUrl(kefuUrl);
                        tMsgKefu.setCreateTime(now);
                        tMsgKefu.setModifiedTime(now);

                        if (existSameMsg) {
                            msgKefuMapper.updateByMsgTypeAndMsgName(tMsgKefu);
                        } else {
                            msgKefuMapper.insertSelective(tMsgKefu);
                            msgId = tMsgKefu.getId();
                        }
                    } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
                        TMsgKefuPriority tMsgKefuPriority = new TMsgKefuPriority();
                        tMsgKefuPriority.setMsgType(msgType);
                        tMsgKefuPriority.setMsgName(msgName);
                        tMsgKefuPriority.setTemplateId(templateId);
                        tMsgKefuPriority.setUrl(templateUrl);
                        tMsgKefuPriority.setMaAppid(templateMiniAppid);
                        tMsgKefuPriority.setMaPagePath(templateMiniPagePath);
                        tMsgKefuPriority.setKefuMsgType(kefuMsgType);
                        tMsgKefuPriority.setContent(kefuMsgTitle);
                        tMsgKefuPriority.setTitle(kefuMsgTitle);
                        tMsgKefuPriority.setImgUrl(kefuPicUrl);
                        tMsgKefuPriority.setDescribe(kefuDesc);
                        tMsgKefuPriority.setKefuUrl(kefuUrl);
                        tMsgKefuPriority.setCreateTime(now);
                        tMsgKefuPriority.setModifiedTime(now);

                        if (existSameMsg) {
                            msgKefuPriorityMapper.updateByMsgTypeAndMsgName(tMsgKefuPriority);
                        } else {
                            msgKefuPriorityMapper.insertSelective(tMsgKefuPriority);
                            msgId = tMsgKefuPriority.getId();
                        }
                    } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                        TMsgMaTemplate tMsgMaTemplate = new TMsgMaTemplate();
                        tMsgMaTemplate.setMsgType(msgType);
                        tMsgMaTemplate.setMsgName(msgName);
                        tMsgMaTemplate.setTemplateId(templateId);
                        tMsgMaTemplate.setPage(templateUrl);
                        tMsgMaTemplate.setEmphasisKeyword(templateKeyWord);
                        tMsgMaTemplate.setCreateTime(now);
                        tMsgMaTemplate.setModifiedTime(now);

                        if (existSameMsg) {
                            msgMaTemplateMapper.updateByMsgTypeAndMsgName(tMsgMaTemplate);
                        } else {
                            msgMaTemplateMapper.insertSelective(tMsgMaTemplate);
                            msgId = tMsgMaTemplate.getId();
                        }
                    } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
                        TMsgMpTemplate tMsgMpTemplate = new TMsgMpTemplate();
                        tMsgMpTemplate.setMsgType(msgType);
                        tMsgMpTemplate.setMsgName(msgName);
                        tMsgMpTemplate.setTemplateId(templateId);
                        tMsgMpTemplate.setUrl(templateUrl);
                        tMsgMpTemplate.setMaAppid(templateMiniAppid);
                        tMsgMpTemplate.setMaPagePath(templateMiniPagePath);
                        tMsgMpTemplate.setCreateTime(now);
                        tMsgMpTemplate.setModifiedTime(now);

                        if (existSameMsg) {
                            msgMpTemplateMapper.updateByMsgTypeAndMsgName(tMsgMpTemplate);
                        } else {
                            msgMpTemplateMapper.insertSelective(tMsgMpTemplate);
                            msgId = tMsgMpTemplate.getId();
                        }
                    } else {
                        TMsgSms tMsgSms = new TMsgSms();
                        tMsgSms.setMsgType(msgType);
                        tMsgSms.setMsgName(msgName);
                        tMsgSms.setTemplateId(templateId);
                        tMsgSms.setContent(yunpianMsgContent);
                        tMsgSms.setCreateTime(now);
                        tMsgSms.setModifiedTime(now);

                        if (existSameMsg) {
                            msgSmsMapper.updateByMsgTypeAndMsgName(tMsgSms);
                        } else {
                            msgSmsMapper.insertSelective(tMsgSms);
                            msgId = tMsgSms.getId();
                        }
                    }

                    // 保存模板数据
                    if (msgType != MessageTypeEnum.KEFU_CODE && msgType != MessageTypeEnum.YUN_PIAN_CODE) {

                        // 如果是覆盖保存，则先清空之前的模板数据
                        if (existSameMsg) {
                            templateDataMapper.deleteByMsgTypeAndMsgId(msgType, msgId);
                        }

                        // 如果table为空，则初始化
                        if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                            MessageEditForm.initTemplateDataTable();
                        }

                        // 逐行读取
                        DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable()
                                .getModel();
                        int rowCount = tableModel.getRowCount();
                        for (int i = 0; i < rowCount; i++) {
                            String name = (String) tableModel.getValueAt(i, 0);
                            String value = (String) tableModel.getValueAt(i, 1);
                            String color = ((String) tableModel.getValueAt(i, 2)).trim();

                            TTemplateData tTemplateData = new TTemplateData();
                            tTemplateData.setMsgType(msgType);
                            tTemplateData.setMsgId(msgId);
                            tTemplateData.setName(name);
                            tTemplateData.setValue(value);
                            tTemplateData.setColor(color);
                            tTemplateData.setCreateTime(now);
                            tTemplateData.setModifiedTime(now);

                            templateDataMapper.insert(tTemplateData);
                        }

                    }

                    App.config.setPreviewUser(MessageEditForm.messageEditForm.getPreviewUserField().getText());
                    App.config.save();

                    JOptionPane.showMessageDialog(messagePanel, "保存成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);

                    MessageEditForm.init(null);
                    MessageManageForm.init();
                }
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

        // 编辑消息-新建
        MessageManageForm.messageManageForm.getCreateMsgButton().addActionListener(e -> {
            MessageEditForm.clearAllField();
            MessageEditForm.initTemplateDataTable();
            MessageEditForm.messageEditForm.getMsgNameField().grabFocus();
        });
    }
}
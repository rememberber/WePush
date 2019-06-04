package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.domain.TTemplateData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.form.msg.AliTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.AliYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.KefuMsgForm;
import com.fangxuele.tool.push.ui.form.msg.MaTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.msg.MpTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.TxYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.YunpianMsgForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;

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

    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

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
                if (msgType == MessageTypeEnum.KEFU_CODE) {
                    KefuMsgForm.save(msgName);
                } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
                    int msgId = 0;
                    boolean existSameMsg = false;

                    List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndMsgName(msgType, msgName);
                    if (tMsgKefuPriorityList.size() > 0) {
                        existSameMsg = true;
                        msgId = tMsgKefuPriorityList.get(0).getId();
                    }

                    int isCover = JOptionPane.NO_OPTION;
                    if (existSameMsg) {
                        // 如果存在，是否覆盖
                        isCover = JOptionPane.showConfirmDialog(messagePanel, "已经存在同名的历史消息，\n是否覆盖？", "确认",
                                JOptionPane.YES_NO_OPTION);
                    }

                    if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
                        String templateId = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateIdTextField().getText();
                        String templateUrl = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateUrlTextField().getText();
                        String kefuMsgType = Objects.requireNonNull(KefuMsgForm.kefuMsgForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
                        String kefuMsgTitle = KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().getText();
                        String kefuPicUrl = KefuMsgForm.kefuMsgForm.getMsgKefuPicUrlTextField().getText();
                        String kefuDesc = KefuMsgForm.kefuMsgForm.getMsgKefuDescTextField().getText();
                        String kefuUrl = KefuMsgForm.kefuMsgForm.getMsgKefuUrlTextField().getText();
                        String templateMiniAppid = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniAppidTextField().getText();
                        String templateMiniPagePath = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniPagePathTextField().getText();

                        String now = SqliteUtil.nowDateForSqlite();

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

                        // 保存模板数据
                        // 如果是覆盖保存，则先清空之前的模板数据
                        if (existSameMsg) {
                            templateDataMapper.deleteByMsgTypeAndMsgId(msgType, msgId);
                        }

                        // 如果table为空，则初始化
                        if (MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                            MpTemplateMsgForm.initTemplateDataTable();
                        }

                        // 逐行读取
                        DefaultTableModel tableModel = (DefaultTableModel) MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable()
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

                } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                    MaTemplateMsgForm.save(msgName);
                } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
                    MpTemplateMsgForm.save(msgName);
                } else if (msgType == MessageTypeEnum.ALI_TEMPLATE_CODE) {
                    AliTemplateMsgForm.save(msgName);
                } else if (msgType == MessageTypeEnum.ALI_YUN_CODE) {
                    AliYunMsgForm.save(msgName);
                } else if (msgType == MessageTypeEnum.TX_YUN_CODE) {
                    TxYunMsgForm.save(msgName);
                } else if (msgType == MessageTypeEnum.YUN_PIAN_CODE) {
                    YunpianMsgForm.save(msgName);
                }


                App.config.setPreviewUser(MessageEditForm.messageEditForm.getPreviewUserField().getText());
                App.config.save();

                JOptionPane.showMessageDialog(messagePanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);

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
    }
}
package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 * 客服消息优先(伪form)
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/6.
 */
public class KefuPriorityMsgForm implements IMsgForm {

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private static KefuPriorityMsgForm kefuPriorityMsgForm;

    @Override
    public void init(Integer msgId) {
        clearAllField();
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        if (tMsg != null) {
            TMsgKefuPriority tMsgKefuPriority = JSONUtil.toBean(tMsg.getContent(), TMsgKefuPriority.class);
            MpTemplateMsgForm.getInstance().getMsgTemplateIdTextField().setText(tMsgKefuPriority.getTemplateId());
            MpTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().setText(tMsgKefuPriority.getUrl());
            MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().setText(tMsgKefuPriority.getMaAppid());
            MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().setText(tMsgKefuPriority.getMaPagePath());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsg.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsg.getPreviewUser());

            String kefuMsgType = tMsgKefuPriority.getKefuMsgType();
            KefuMsgForm.getInstance().getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
            if ("文本消息".equals(kefuMsgType)) {
                KefuMsgForm.getInstance().getContentTextArea().setText(tMsg.getContent());
            } else if ("图文消息".equals(kefuMsgType)) {
                KefuMsgForm.getInstance().getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getTitle());
            } else if ("小程序卡片消息".equals(kefuMsgType)) {
                KefuMsgForm.getInstance().getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getTitle());
                KefuMsgForm.getInstance().getMsgKefuAppidTextField().setText(tMsgKefuPriority.getAppId());
                KefuMsgForm.getInstance().getMsgKefuPagepathTextField().setText(tMsgKefuPriority.getPagePath());
                KefuMsgForm.getInstance().getMsgKefuThumbMediaIdTextField().setText(tMsgKefuPriority.getThumbMediaId());
            }
            KefuMsgForm.getInstance().getMsgKefuPicUrlTextField().setText(tMsgKefuPriority.getImgUrl());
            KefuMsgForm.getInstance().getMsgKefuDescTextField().setText(tMsgKefuPriority.getDescribe());
            KefuMsgForm.getInstance().getMsgKefuUrlTextField().setText(tMsgKefuPriority.getKefuUrl());

            KefuMsgForm.switchKefuMsgType(kefuMsgType);

            // 模板消息Data表
            List<TemplateData> templateDataList = tMsgKefuPriority.getTemplateDataList();
            String[] headerNames = {"Name", "Value", "Color", "操作"};
            Object[][] cellData = new String[templateDataList.size()][headerNames.length];
            for (int i = 0; i < templateDataList.size(); i++) {
                TemplateData tTemplateData = templateDataList.get(i);
                cellData[i][0] = tTemplateData.getName();
                cellData[i][1] = tTemplateData.getValue();
                cellData[i][2] = tTemplateData.getColor();
            }
            DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
            MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().setModel(model);
            TableColumnModel tableColumnModel = MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().getColumnModel();
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellRenderer(new TableInCellButtonColumn(MpTemplateMsgForm.getInstance().getTemplateMsgDataTable(), headerNames.length - 1));
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellEditor(new TableInCellButtonColumn(MpTemplateMsgForm.getInstance().getTemplateMsgDataTable(), headerNames.length - 1));

            // 设置列宽
            tableColumnModel.getColumn(3).setPreferredWidth(46);
            tableColumnModel.getColumn(3).setMaxWidth(46);

            MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().updateUI();
        } else {
            KefuMsgForm.switchKefuMsgType("图文消息");
        }
        MpTemplateMsgForm.initTemplateList();
    }

    @Override
    public void save(Integer accountId, String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        TMsg tMsg = msgMapper.selectByUnique(MessageTypeEnum.KEFU_PRIORITY_CODE, accountId, msgName);
        if (tMsg != null) {
            existSameMsg = true;
            msgId = tMsg.getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String templateId = MpTemplateMsgForm.getInstance().getMsgTemplateIdTextField().getText();
            String templateUrl = MpTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().getText();
            String kefuMsgType = Objects.requireNonNull(KefuMsgForm.getInstance().getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
            String kefuMsgContent = KefuMsgForm.getInstance().getContentTextArea().getText();
            String kefuMsgTitle = KefuMsgForm.getInstance().getMsgKefuMsgTitleTextField().getText();
            String kefuPicUrl = KefuMsgForm.getInstance().getMsgKefuPicUrlTextField().getText();
            String kefuDesc = KefuMsgForm.getInstance().getMsgKefuDescTextField().getText();
            String kefuUrl = KefuMsgForm.getInstance().getMsgKefuUrlTextField().getText();
            String kefuAppId = KefuMsgForm.getInstance().getMsgKefuAppidTextField().getText();
            String kefuPagePath = KefuMsgForm.getInstance().getMsgKefuPagepathTextField().getText();
            String kefuThumbMediaId = KefuMsgForm.getInstance().getMsgKefuThumbMediaIdTextField().getText();
            String templateMiniAppid = MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().getText();
            String templateMiniPagePath = MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsg msg = new TMsg();
            TMsgKefuPriority tMsgKefuPriority = new TMsgKefuPriority();
            msg.setMsgType(MessageTypeEnum.KEFU_PRIORITY_CODE);
            msg.setAccountId(accountId);
            msg.setMsgName(msgName);
            tMsgKefuPriority.setTemplateId(templateId);
            tMsgKefuPriority.setUrl(templateUrl);
            tMsgKefuPriority.setMaAppid(templateMiniAppid);
            tMsgKefuPriority.setMaPagePath(templateMiniPagePath);
            tMsgKefuPriority.setKefuMsgType(kefuMsgType);
            tMsgKefuPriority.setContent(kefuMsgContent);
            tMsgKefuPriority.setTitle(kefuMsgTitle);
            tMsgKefuPriority.setImgUrl(kefuPicUrl);
            tMsgKefuPriority.setDescribe(kefuDesc);
            tMsgKefuPriority.setKefuUrl(kefuUrl);
            msg.setCreateTime(now);
            msg.setModifiedTime(now);
            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            msg.setPreviewUser(messageEditForm.getPreviewUserField().getText());
            tMsgKefuPriority.setAppId(kefuAppId);
            tMsgKefuPriority.setPagePath(kefuPagePath);
            tMsgKefuPriority.setThumbMediaId(kefuThumbMediaId);

            // 如果table为空，则初始化
            if (MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MpTemplateMsgForm.initTemplateDataTable();
            }

            // 逐行读取
            DefaultTableModel tableModel = (DefaultTableModel) MpTemplateMsgForm.getInstance().getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            List<TemplateData> templateDataList = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String value = (String) tableModel.getValueAt(i, 1);
                String color = ((String) tableModel.getValueAt(i, 2)).trim();

                TemplateData tTemplateData = new TemplateData();
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setColor(color);

                templateDataList.add(tTemplateData);
            }
            tMsgKefuPriority.setTemplateDataList(templateDataList);

            msg.setContent(JSON.toJSONString(tMsgKefuPriority));
            if (existSameMsg) {
                msg.setId(msgId);
                msgMapper.updateByPrimaryKeySelective(msg);
            } else {
                msgMapper.insertSelective(msg);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static KefuPriorityMsgForm getInstance() {
        if (kefuPriorityMsgForm == null) {
            kefuPriorityMsgForm = new KefuPriorityMsgForm();
        }
        return kefuPriorityMsgForm;
    }

    /**
     * 清空所有界面字段
     */
    @Override
    public void clearAllField() {
        KefuMsgForm.getInstance().clearAllField();
        MpTemplateMsgForm.getInstance().clearAllField();
    }
}

package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.domain.TTemplateData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
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
public class KefuPriorityMsgForm {

    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    public static void init(String msgName) {
        clearAllField();
        List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_PRIORITY_CODE, msgName);
        if (tMsgKefuPriorityList.size() > 0) {
            TMsgKefuPriority tMsgKefuPriority = tMsgKefuPriorityList.get(0);
            Integer msgId = tMsgKefuPriority.getId();
            MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateIdTextField().setText(tMsgKefuPriority.getTemplateId());
            MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateUrlTextField().setText(tMsgKefuPriority.getUrl());
            MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniAppidTextField().setText(tMsgKefuPriority.getMaAppid());
            MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniPagePathTextField().setText(tMsgKefuPriority.getMaPagePath());

            String kefuMsgType = tMsgKefuPriority.getKefuMsgType();
            KefuMsgForm.kefuMsgForm.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
            if ("文本消息".equals(kefuMsgType)) {
                KefuMsgForm.kefuMsgForm.getContentTextArea().setText(tMsgKefuPriority.getContent());
            } else if ("图文消息".equals(kefuMsgType)) {
                KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getTitle());
            }
            KefuMsgForm.kefuMsgForm.getMsgKefuPicUrlTextField().setText(tMsgKefuPriority.getImgUrl());
            KefuMsgForm.kefuMsgForm.getMsgKefuDescTextField().setText(tMsgKefuPriority.getDescribe());
            KefuMsgForm.kefuMsgForm.getMsgKefuUrlTextField().setText(tMsgKefuPriority.getKefuUrl());

            KefuMsgForm.switchKefuMsgType(kefuMsgType);

            MpTemplateMsgForm.selectedMsgTemplateId = tMsgKefuPriority.getTemplateId();
            // 模板消息Data表
            List<TTemplateData> templateDataList = templateDataMapper.selectByMsgTypeAndMsgId(MessageTypeEnum.KEFU_PRIORITY_CODE, msgId);
            String[] headerNames = {"Name", "Value", "Color", "操作"};
            Object[][] cellData = new String[templateDataList.size()][headerNames.length];
            for (int i = 0; i < templateDataList.size(); i++) {
                TTemplateData tTemplateData = templateDataList.get(i);
                cellData[i][0] = tTemplateData.getName();
                cellData[i][1] = tTemplateData.getValue();
                cellData[i][2] = tTemplateData.getColor();
            }
            DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
            MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().setModel(model);
            TableColumnModel tableColumnModel = MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().getColumnModel();
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellRenderer(new TableInCellButtonColumn(MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable(), headerNames.length - 1));
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellEditor(new TableInCellButtonColumn(MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable(), headerNames.length - 1));

            // 设置列宽
            tableColumnModel.getColumn(3).setPreferredWidth(130);
            tableColumnModel.getColumn(3).setMaxWidth(130);

            MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().updateUI();
        } else {
            KefuMsgForm.switchKefuMsgType("图文消息");
        }
        MpTemplateMsgForm.initTemplateList();
    }

    public static void save(String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_PRIORITY_CODE, msgName);
        if (tMsgKefuPriorityList.size() > 0) {
            existSameMsg = true;
            msgId = tMsgKefuPriorityList.get(0).getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String templateId = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateIdTextField().getText();
            String templateUrl = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateUrlTextField().getText();
            String kefuMsgType = Objects.requireNonNull(KefuMsgForm.kefuMsgForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
            String kefuMsgContent = KefuMsgForm.kefuMsgForm.getContentTextArea().getText();
            String kefuMsgTitle = KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().getText();
            String kefuPicUrl = KefuMsgForm.kefuMsgForm.getMsgKefuPicUrlTextField().getText();
            String kefuDesc = KefuMsgForm.kefuMsgForm.getMsgKefuDescTextField().getText();
            String kefuUrl = KefuMsgForm.kefuMsgForm.getMsgKefuUrlTextField().getText();
            String templateMiniAppid = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniAppidTextField().getText();
            String templateMiniPagePath = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniPagePathTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgKefuPriority tMsgKefuPriority = new TMsgKefuPriority();
            tMsgKefuPriority.setMsgType(MessageTypeEnum.KEFU_PRIORITY_CODE);
            tMsgKefuPriority.setMsgName(msgName);
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
                templateDataMapper.deleteByMsgTypeAndMsgId(MessageTypeEnum.KEFU_PRIORITY_CODE, msgId);
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
                tTemplateData.setMsgType(MessageTypeEnum.KEFU_PRIORITY_CODE);
                tTemplateData.setMsgId(msgId);
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setColor(color);
                tTemplateData.setCreateTime(now);
                tTemplateData.setModifiedTime(now);

                templateDataMapper.insert(tTemplateData);
            }

            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        KefuMsgForm.clearAllField();
        MpTemplateMsgForm.clearAllField();
    }
}

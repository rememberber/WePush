package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TMsgWxUniformMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgWxUniform;
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
public class WxUniformMsgForm implements IMsgForm {

    private static TMsgWxUniformMapper msgWxUniformMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxUniformMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    private static WxUniformMsgForm wxUniformMsgForm;

    @Override
    public void init(String msgName) {
        clearAllField();
        List<TMsgWxUniform> tMsgWxUniformList = msgWxUniformMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE, msgName);
        if (tMsgWxUniformList.size() > 0) {
            TMsgWxUniform tMsgWxUniform = tMsgWxUniformList.get(0);
            Integer msgId = tMsgWxUniform.getId();
            MpTemplateMsgForm.getInstance().getMsgTemplateIdTextField().setText(tMsgWxUniform.getMpTemplateId());
            MpTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().setText(tMsgWxUniform.getMpUrl());
            MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().setText(tMsgWxUniform.getMaAppid());
            MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().setText(tMsgWxUniform.getMaPagePath());

            MaTemplateMsgForm.getInstance().getMsgTemplateIdTextField().setText(tMsgWxUniform.getMaTemplateId());
            MaTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().setText(tMsgWxUniform.getPage());
            MaTemplateMsgForm.getInstance().getMsgTemplateKeyWordTextField().setText(tMsgWxUniform.getEmphasisKeyword());

            // -------------公众号模板数据开始
            MpTemplateMsgForm.selectedMsgTemplateId = tMsgWxUniform.getMpTemplateId();
            // 模板消息Data表
            List<TTemplateData> templateDataList = templateDataMapper.selectByMsgTypeAndMsgId(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE * MessageTypeEnum.MP_TEMPLATE_CODE, msgId);
            String[] headerNames = {"Name", "Value", "Color", "操作"};
            Object[][] cellData = new String[templateDataList.size()][headerNames.length];
            for (int i = 0; i < templateDataList.size(); i++) {
                TTemplateData tTemplateData = templateDataList.get(i);
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
            // -------------公众号模板数据结束

            // -------------小程序模板数据开始
            MaTemplateMsgForm.initTemplateDataTable();
            // 模板消息Data表
            templateDataList = templateDataMapper.selectByMsgTypeAndMsgId(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE * MessageTypeEnum.MA_TEMPLATE_CODE, msgId);
            for (int i = 0; i < templateDataList.size(); i++) {
                TTemplateData tTemplateData = templateDataList.get(i);
                cellData[i][0] = tTemplateData.getName();
                cellData[i][1] = tTemplateData.getValue();
                cellData[i][2] = tTemplateData.getColor();
            }
            model = new DefaultTableModel(cellData, headerNames);
            MaTemplateMsgForm.getInstance().getTemplateMsgDataTable().setModel(model);
            tableColumnModel = MaTemplateMsgForm.getInstance().getTemplateMsgDataTable().getColumnModel();
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellRenderer(new TableInCellButtonColumn(MaTemplateMsgForm.getInstance().getTemplateMsgDataTable(), headerNames.length - 1));
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellEditor(new TableInCellButtonColumn(MaTemplateMsgForm.getInstance().getTemplateMsgDataTable(), headerNames.length - 1));

            // 设置列宽
            tableColumnModel.getColumn(3).setPreferredWidth(46);
            tableColumnModel.getColumn(3).setMaxWidth(46);
            // -------------小程序模板数据结束

        }
        MpTemplateMsgForm.initTemplateList();
    }

    @Override
    public void save(String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        List<TMsgWxUniform> tMsgWxUniformList = msgWxUniformMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE, msgName);
        if (tMsgWxUniformList.size() > 0) {
            existSameMsg = true;
            msgId = tMsgWxUniformList.get(0).getId();
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
            String templateMiniAppid = MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().getText();
            String templateMiniPagePath = MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgWxUniform tMsgWxUniform = new TMsgWxUniform();
            tMsgWxUniform.setMsgType(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE);
            tMsgWxUniform.setMsgName(msgName);
            tMsgWxUniform.setMpTemplateId(templateId);
            tMsgWxUniform.setMpUrl(templateUrl);
            tMsgWxUniform.setMaAppid(templateMiniAppid);
            tMsgWxUniform.setMaPagePath(templateMiniPagePath);
            tMsgWxUniform.setKefuMsgType(kefuMsgType);
            tMsgWxUniform.setContent(kefuMsgContent);
            tMsgWxUniform.setTitle(kefuMsgTitle);
            tMsgWxUniform.setImgUrl(kefuPicUrl);
            tMsgWxUniform.setDescribe(kefuDesc);
            tMsgWxUniform.setKefuUrl(kefuUrl);
            tMsgWxUniform.setCreateTime(now);
            tMsgWxUniform.setModifiedTime(now);

            if (existSameMsg) {
                msgWxUniformMapper.updateByMsgTypeAndMsgName(tMsgWxUniform);
            } else {
                msgWxUniformMapper.insertSelective(tMsgWxUniform);
                msgId = tMsgWxUniform.getId();
            }

            // 保存模板数据
            // 如果是覆盖保存，则先清空之前的模板数据
            if (existSameMsg) {
                templateDataMapper.deleteByMsgTypeAndMsgId(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE, msgId);
            }

            // 如果table为空，则初始化
            if (MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MpTemplateMsgForm.initTemplateDataTable();
            }

            // 逐行读取
            DefaultTableModel tableModel = (DefaultTableModel) MpTemplateMsgForm.getInstance().getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String value = (String) tableModel.getValueAt(i, 1);
                String color = ((String) tableModel.getValueAt(i, 2)).trim();

                TTemplateData tTemplateData = new TTemplateData();
                tTemplateData.setMsgType(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE * MessageTypeEnum.MP_TEMPLATE_CODE);
                tTemplateData.setMsgId(msgId);
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setColor(color);
                tTemplateData.setCreateTime(now);
                tTemplateData.setModifiedTime(now);

                templateDataMapper.insert(tTemplateData);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static WxUniformMsgForm getInstance() {
        if (wxUniformMsgForm == null) {
            wxUniformMsgForm = new WxUniformMsgForm();
        }
        return wxUniformMsgForm;
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        MaTemplateMsgForm.clearAllField();
        MpTemplateMsgForm.clearAllField();
    }
}

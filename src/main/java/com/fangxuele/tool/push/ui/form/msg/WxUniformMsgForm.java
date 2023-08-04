package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgWxUniform;
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

/**
 * <pre>
 * 微信统一服务消息(伪form)
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/29.
 */
public class WxUniformMsgForm implements IMsgForm {

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);
    private static WxUniformMsgForm wxUniformMsgForm;

    @Override
    public void init(Integer msgId) {
        clearAllField();
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        if (tMsg != null) {
            TMsgWxUniform tMsgWxUniform = JSONUtil.toBean(tMsg.getContent(), TMsgWxUniform.class);
            MpTemplateMsgForm.getInstance().getMsgTemplateIdTextField().setText(tMsgWxUniform.getMpTemplateId());
            MpTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().setText(tMsgWxUniform.getMpUrl());
            MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().setText(tMsgWxUniform.getMaAppid());
            MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().setText(tMsgWxUniform.getMaPagePath());

            MaSubscribeMsgForm.getInstance().getMsgTemplateIdTextField().setText(tMsgWxUniform.getMaTemplateId());
            MaSubscribeMsgForm.getInstance().getMsgTemplateUrlTextField().setText(tMsgWxUniform.getPage());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsg.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsg.getPreviewUser());

            // -------------公众号模板数据开始
            MpTemplateMsgForm.selectedMsgTemplateId = tMsgWxUniform.getMpTemplateId();
            // 模板消息Data表
            List<TemplateData> templateDataList = tMsgWxUniform.getTemplateDataListMp();
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
            // -------------公众号模板数据结束

            // -------------小程序模板数据开始
            MaSubscribeMsgForm.initTemplateDataTable();
            // 模板消息Data表
            templateDataList = tMsgWxUniform.getTemplateDataListMa();
            for (int i = 0; i < templateDataList.size(); i++) {
                TemplateData tTemplateData = templateDataList.get(i);
                cellData[i][0] = tTemplateData.getName();
                cellData[i][1] = tTemplateData.getValue();
                cellData[i][2] = tTemplateData.getColor();
            }
            model = new DefaultTableModel(cellData, headerNames);
            MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable().setModel(model);
            tableColumnModel = MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable().getColumnModel();
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellRenderer(new TableInCellButtonColumn(MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable(), headerNames.length - 1));
            tableColumnModel.getColumn(headerNames.length - 1).
                    setCellEditor(new TableInCellButtonColumn(MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable(), headerNames.length - 1));

            // 设置列宽
            tableColumnModel.getColumn(3).setPreferredWidth(46);
            tableColumnModel.getColumn(3).setMaxWidth(46);
            // -------------小程序模板数据结束

        }
        MpTemplateMsgForm.initTemplateList();
    }

    @Override
    public void save(Integer accountId, String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        TMsg tMsg = msgMapper.selectByUnique(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE, accountId, msgName);
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
            String mpTemplateId = MpTemplateMsgForm.getInstance().getMsgTemplateIdTextField().getText();
            String templateUrl = MpTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().getText();
            String templateMiniAppid = MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().getText();
            String templateMiniPagePath = MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().getText();

            String maTemplateId = MaSubscribeMsgForm.getInstance().getMsgTemplateIdTextField().getText();
            String page = MaSubscribeMsgForm.getInstance().getMsgTemplateUrlTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsg msg = new TMsg();
            TMsgWxUniform tMsgWxUniform = new TMsgWxUniform();
            msg.setMsgType(MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE);
            msg.setAccountId(accountId);
            msg.setMsgName(msgName);
            tMsgWxUniform.setMpTemplateId(mpTemplateId);
            tMsgWxUniform.setMaTemplateId(maTemplateId);
            tMsgWxUniform.setMpUrl(templateUrl);
            tMsgWxUniform.setMaAppid(templateMiniAppid);
            tMsgWxUniform.setMaPagePath(templateMiniPagePath);
            tMsgWxUniform.setPage(page);
            msg.setCreateTime(now);
            msg.setModifiedTime(now);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            msg.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            // -------------公众号模板数据开始
            // 如果table为空，则初始化
            if (MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MpTemplateMsgForm.initTemplateDataTable();
            }

            // 逐行读取
            DefaultTableModel tableModel = (DefaultTableModel) MpTemplateMsgForm.getInstance().getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            List<TemplateData> templateDataListMp = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String value = (String) tableModel.getValueAt(i, 1);
                String color = ((String) tableModel.getValueAt(i, 2)).trim();

                TemplateData tTemplateData = new TemplateData();
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setColor(color);

                templateDataListMp.add(tTemplateData);
            }
            // -------------公众号模板数据结束

            // -------------小程序模板数据开始
            // 如果table为空，则初始化
            if (MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MaSubscribeMsgForm.initTemplateDataTable();
            }

            // 逐行读取
            tableModel = (DefaultTableModel) MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable()
                    .getModel();
            rowCount = tableModel.getRowCount();
            List<TemplateData> templateDataListMa = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String value = (String) tableModel.getValueAt(i, 1);
                String color = ((String) tableModel.getValueAt(i, 2)).trim();

                TemplateData tTemplateData = new TemplateData();
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setColor(color);

                templateDataListMa.add(tTemplateData);
            }

            tMsgWxUniform.setTemplateDataListMp(templateDataListMp);
            tMsgWxUniform.setTemplateDataListMa(templateDataListMa);

            msg.setContent(JSON.toJSONString(tMsgWxUniform));
            if (existSameMsg) {
                msg.setId(msgId);
                msgMapper.updateByPrimaryKeySelective(msg);
            } else {
                msgMapper.insertSelective(msg);
            }

            // -------------小程序模板数据结束
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
    @Override
    public void clearAllField() {
        MaSubscribeMsgForm.getInstance().clearAllField();
        MpTemplateMsgForm.getInstance().clearAllField();
    }
}

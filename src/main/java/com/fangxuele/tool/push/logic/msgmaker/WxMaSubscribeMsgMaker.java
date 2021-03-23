package com.fangxuele.tool.push.logic.msgmaker;

import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.ui.form.msg.MaSubscribeMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * <pre>
 * 小程序订阅消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/11/29.
 */
public class WxMaSubscribeMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String templateId;
    private static String templateUrl;
    public static List<TemplateData> templateDataList;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = MaSubscribeMsgForm.getInstance().getMsgTemplateIdTextField().getText().trim();
        templateUrl = MaSubscribeMsgForm.getInstance().getMsgTemplateUrlTextField().getText().trim();

        if (MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            MaSubscribeMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MaSubscribeMsgForm.getInstance().getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        TemplateData templateData;
        templateDataList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) tableModel.getValueAt(i, 0)).trim();
            String value = ((String) tableModel.getValueAt(i, 1)).trim();
            String color = ((String) tableModel.getValueAt(i, 2)).trim();
            templateData = new TemplateData();
            templateData.setName(name);
            templateData.setValue(value);
            templateData.setColor(color);
            templateDataList.add(templateData);
        }
    }

    /**
     * 组织订阅消息-小程序
     *
     * @param msgData 消息信息
     * @return WxMaTemplateMessage
     */
    @Override
    public WxMaSubscribeMessage makeMsg(String[] msgData) {
        // 拼模板
        WxMaSubscribeMessage wxMaSubscribeMessage = new WxMaSubscribeMessage();
        wxMaSubscribeMessage.setTemplateId(templateId);
        VelocityContext velocityContext = getVelocityContext(msgData);
        String templateUrlEvaluated = TemplateUtil.evaluate(templateUrl, velocityContext);
        wxMaSubscribeMessage.setPage(templateUrlEvaluated);

        WxMaSubscribeMessage.MsgData wxMaSubscribeData;
        for (TemplateData templateData : templateDataList) {
            wxMaSubscribeData = new WxMaSubscribeMessage.MsgData();
            wxMaSubscribeData.setName(templateData.getName());
            wxMaSubscribeData.setValue(TemplateUtil.evaluate(templateData.getValue(), velocityContext));
            wxMaSubscribeMessage.addData(wxMaSubscribeData);
        }

        return wxMaSubscribeMessage;
    }
}

package com.fangxuele.tool.push.logic.msgmaker;

import cn.binarywang.wx.miniapp.bean.WxMaTemplateData;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.logic.msgsender.WxMaTemplateMsgSender;
import com.fangxuele.tool.push.ui.form.msg.MaTemplateMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * <pre>
 * 小程序模板消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class WxMaTemplateMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String templateId;
    public static String templateUrl;
    public static String templateKeyWord;
    public static List<TemplateData> templateDataList;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = MaTemplateMsgForm.getInstance().getMsgTemplateIdTextField().getText().trim();
        templateUrl = MaTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().getText().trim();
        templateKeyWord = MaTemplateMsgForm.getInstance().getMsgTemplateKeyWordTextField().getText().trim() + ".DATA";

        if (MaTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            MaTemplateMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MaTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel();
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
        WxMaTemplateMsgSender.wxMaConfigStorage = null;
        WxMaTemplateMsgSender.wxMaService = null;
    }

    /**
     * 组织模板消息-小程序
     *
     * @param msgData 消息信息
     * @return WxMaTemplateMessage
     */
    @Override
    public WxMaTemplateMessage makeMsg(String[] msgData) {
        // 拼模板
        WxMaTemplateMessage wxMessageTemplate = new WxMaTemplateMessage();
        wxMessageTemplate.setTemplateId(templateId);
        wxMessageTemplate.setPage(templateUrl);
        wxMessageTemplate.setEmphasisKeyword(templateKeyWord);

        VelocityContext velocityContext = getVelocityContext(msgData);

        WxMaTemplateData wxMaTemplateData;
        for (TemplateData templateData : templateDataList) {
            wxMaTemplateData = new WxMaTemplateData();
            wxMaTemplateData.setName(templateData.getName());
            wxMaTemplateData.setValue(TemplateUtil.evaluate(templateData.getValue(), velocityContext));
            wxMaTemplateData.setColor(templateData.getColor());
            wxMessageTemplate.addData(wxMaTemplateData);
        }

        return wxMessageTemplate;
    }
}

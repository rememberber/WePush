package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.form.msg.MpTemplateMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * <pre>
 * 公众号模板消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/13.
 */
public class MpTemplateMsgMaker {

    public static String templateId;
    public static String templateUrl;
    public static String miniAppId;
    public static String miniAppPagePath;
    public static List<TemplateData> templateDataList;

    /**
     * 准备(界面字段等)
     */
    public static void prepare() {
        templateId = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateIdTextField().getText().trim();
        templateUrl = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateUrlTextField().getText().trim();
        miniAppId = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniAppidTextField().getText().trim();
        miniAppPagePath = MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniPagePathTextField().getText().trim();

        if (MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            MpTemplateMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().getModel();
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
     * 组织模板消息-公众号
     *
     * @param msgData 消息数据
     * @return WxMpTemplateMessage
     */
    public WxMpTemplateMessage makeMsg(String[] msgData) {
        // 拼模板
        WxMpTemplateMessage wxMessageTemplate = new WxMpTemplateMessage();
        wxMessageTemplate.setTemplateId(templateId);
        wxMessageTemplate.setUrl(templateUrl);

        VelocityContext velocityContext = getVelocityContext(msgData);
        miniAppPagePath = TemplateUtil.evaluate(miniAppPagePath, velocityContext);
        WxMpTemplateMessage.MiniProgram miniProgram = new WxMpTemplateMessage.MiniProgram(miniAppId, miniAppPagePath, true);
        wxMessageTemplate.setMiniProgram(miniProgram);

        WxMpTemplateData wxMpTemplateData;
        for (TemplateData templateData : templateDataList) {
            wxMpTemplateData = new WxMpTemplateData();
            wxMpTemplateData.setName(templateData.getName());
            wxMpTemplateData.setValue(TemplateUtil.evaluate(templateData.getValue(), velocityContext));
            wxMpTemplateData.setColor(templateData.getColor());
            wxMessageTemplate.addData(wxMpTemplateData);
        }

        return wxMessageTemplate;
    }

    /**
     * 获取模板引擎上下文
     *
     * @param msgData 消息数据
     * @return VelocityContext 模板引擎上下文
     */
    private VelocityContext getVelocityContext(String[] msgData) {
        VelocityContext velocityContext = new VelocityContext();
        for (int i = 1; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        return velocityContext;
    }
}

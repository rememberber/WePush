package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgMpTemplate;
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
public class WxMpTemplateMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private String templateUrl;

    private String miniAppId;

    private String miniAppPagePath;

    private List<TemplateData> templateDataList;

    public WxMpTemplateMsgMaker(TMsg tMsg) {
        TMsgMpTemplate tMsgWxMpTemplate = JSON.parseObject(tMsg.getContent(), TMsgMpTemplate.class);
        this.templateId = tMsgWxMpTemplate.getTemplateId();
        this.templateUrl = tMsgWxMpTemplate.getUrl();
        this.miniAppId = tMsgWxMpTemplate.getMaAppid();
        this.miniAppPagePath = tMsgWxMpTemplate.getMaPagePath();
        this.templateDataList = tMsgWxMpTemplate.getTemplateDataList();
    }

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = MpTemplateMsgForm.getInstance().getMsgTemplateIdTextField().getText().trim();
        templateUrl = MpTemplateMsgForm.getInstance().getMsgTemplateUrlTextField().getText().trim();
        miniAppId = MpTemplateMsgForm.getInstance().getMsgTemplateMiniAppidTextField().getText().trim();
        miniAppPagePath = MpTemplateMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().getText().trim();

        if (MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            MpTemplateMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MpTemplateMsgForm.getInstance().getTemplateMsgDataTable().getModel();
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
    @Override
    public WxMpTemplateMessage makeMsg(String[] msgData) {
        // 拼模板
        WxMpTemplateMessage wxMessageTemplate = new WxMpTemplateMessage();
        wxMessageTemplate.setTemplateId(templateId);

        VelocityContext velocityContext = getVelocityContext(msgData);
        String templateUrlEvaluated = TemplateUtil.evaluate(templateUrl, velocityContext);
        wxMessageTemplate.setUrl(templateUrlEvaluated);
        String miniAppPagePathEvaluated = TemplateUtil.evaluate(miniAppPagePath, velocityContext);
        WxMpTemplateMessage.MiniProgram miniProgram = new WxMpTemplateMessage.MiniProgram(miniAppId, miniAppPagePathEvaluated, false);
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
}

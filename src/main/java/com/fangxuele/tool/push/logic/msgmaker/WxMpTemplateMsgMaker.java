package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgMpTemplate;
import com.fangxuele.tool.push.util.TemplateUtil;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.velocity.VelocityContext;

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

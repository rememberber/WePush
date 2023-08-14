package com.fangxuele.tool.push.logic.msgmaker;

import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgMaSubscribe;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

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

    private String templateId;
    private String page;
    private List<TemplateData> templateDataList;

    public WxMaSubscribeMsgMaker(TMsg tMsg) {
        TMsgMaSubscribe tMsgMaSubscribe = JSON.parseObject(tMsg.getContent(), TMsgMaSubscribe.class);
        this.templateId = tMsgMaSubscribe.getTemplateId();
        this.page = tMsgMaSubscribe.getPage();
        this.templateDataList = tMsgMaSubscribe.getTemplateDataList();
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
        String templateUrlEvaluated = TemplateUtil.evaluate(page, velocityContext);
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

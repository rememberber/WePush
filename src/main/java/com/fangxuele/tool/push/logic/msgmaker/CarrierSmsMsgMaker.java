package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

/** 运营商协议短信文本模板加工器。 */
public class CarrierSmsMsgMaker extends BaseMsgMaker implements IMsgMaker {
    private final String content;

    public CarrierSmsMsgMaker(TMsg tMsg) {
        TMsgSms sms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        content = sms == null ? "" : sms.getContent();
    }

    @Override
    public String makeMsg(String[] msgData) {
        VelocityContext context = getVelocityContext(msgData);
        return TemplateUtil.evaluate(content, context);
    }
}

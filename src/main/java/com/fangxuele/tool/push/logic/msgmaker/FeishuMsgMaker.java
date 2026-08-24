package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgFeishu;
import com.fangxuele.tool.push.util.FeishuBotSupport;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

/**
 * 飞书机器人消息加工器。
 */
public class FeishuMsgMaker extends BaseMsgMaker implements IMsgMaker {
    private final String messageType;
    private final String title;
    private final String content;
    private final String mentionType;

    public FeishuMsgMaker(TMsg tMsg) {
        TMsgFeishu config = JSON.parseObject(tMsg.getContent(), TMsgFeishu.class);
        this.messageType = config.getFeishuMsgType();
        this.title = StringUtils.defaultString(config.getTitle());
        this.content = StringUtils.defaultString(config.getContent());
        this.mentionType = StringUtils.defaultIfBlank(config.getMentionType(), FeishuBotSupport.MENTION_NONE);
    }

    @Override
    public TMsgFeishu makeMsg(String[] msgData) {
        VelocityContext context = getVelocityContext(msgData == null ? new String[0] : msgData);
        TMsgFeishu message = new TMsgFeishu();
        message.setFeishuMsgType(messageType);
        message.setTitle(TemplateUtil.evaluate(title, context));
        message.setContent(TemplateUtil.evaluate(content, context));
        message.setMentionType(mentionType);
        return message;
    }
}

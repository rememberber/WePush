package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

/**
 * <pre>
 * Luosimao短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class LuosimaoMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String msgContent;

    public LuosimaoMsgMaker(TMsg tMsg) {
        TMsgSms tMsgSms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        this.msgContent = tMsgSms.getContent();
    }

    /**
     * 组织Luosimao短信消息
     *
     * @param msgData 消息信息
     * @return String
     */
    @Override
    public String makeMsg(String[] msgData) {
        VelocityContext velocityContext = getVelocityContext(msgData);
        return TemplateUtil.evaluate(msgContent, velocityContext);
    }
}

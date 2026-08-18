package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.JiguangPushMsg;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgJiguangPush;
import com.fangxuele.tool.push.util.TemplateUtil;
import lombok.Getter;
import org.apache.velocity.VelocityContext;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 极光推送消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
@Getter
public class JiguangPushMsgMaker extends BaseMsgMaker implements IMsgMaker {

    /**
     * 目标类型：alias-别名，registration_id-RegistrationId
     */
    private String audienceType;

    private String title;

    private String content;

    private boolean apnsProduction;

    private Map<String, String> extrasMap;

    public JiguangPushMsgMaker(TMsg tMsg) {
        TMsgJiguangPush tMsgJiguangPush = JSON.parseObject(tMsg.getContent(), TMsgJiguangPush.class);
        this.audienceType = tMsgJiguangPush.getAudienceType();
        this.title = tMsgJiguangPush.getTitle();
        this.content = tMsgJiguangPush.getContent();
        this.apnsProduction = tMsgJiguangPush.isApnsProduction();

        extrasMap = new HashMap<>();
        if (tMsgJiguangPush.getExtrasList() != null) {
            for (TemplateData templateData : tMsgJiguangPush.getExtrasList()) {
                extrasMap.put(templateData.getName(), templateData.getValue());
            }
        }
    }

    /**
     * 组织极光推送消息
     *
     * @param msgData 消息信息
     * @return JiguangPushMsg
     */
    @Override
    public JiguangPushMsg makeMsg(String[] msgData) {
        VelocityContext velocityContext = getVelocityContext(msgData);

        JiguangPushMsg jiguangPushMsg = new JiguangPushMsg();
        jiguangPushMsg.setTitle(TemplateUtil.evaluate(title, velocityContext));
        jiguangPushMsg.setContent(TemplateUtil.evaluate(content, velocityContext));

        Map<String, String> evaluatedExtras = new HashMap<>(extrasMap.size());
        for (Map.Entry<String, String> entry : extrasMap.entrySet()) {
            evaluatedExtras.put(entry.getKey(), TemplateUtil.evaluate(entry.getValue(), velocityContext));
        }
        jiguangPushMsg.setExtras(evaluatedExtras);

        return jiguangPushMsg;
    }
}

package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.util.TemplateUtil;
import lombok.Getter;
import org.apache.velocity.VelocityContext;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 七牛云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
@Getter
public class QiNiuYunMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private Map<String, String> paramMap;

    public QiNiuYunMsgMaker(TMsg tMsg) {
        TMsgSms tMsgSms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        this.templateId = tMsgSms.getTemplateId();

        paramMap = new HashMap<>();

        for (TemplateData templateData : tMsgSms.getTemplateDataList()) {
            paramMap.put(templateData.getName(), templateData.getValue());
        }
    }

    /**
     * 组织七牛云短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    @Override
    public Map<String, String> makeMsg(String[] msgData) {

        VelocityContext velocityContext = getVelocityContext(msgData);
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            entry.setValue(TemplateUtil.evaluate(entry.getValue(), velocityContext));
        }
        return paramMap;
    }
}

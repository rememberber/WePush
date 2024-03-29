package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.util.TemplateUtil;
import lombok.Getter;
import org.apache.velocity.VelocityContext;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 华为云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
@Getter
public class HwYunMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private List<String> paramList;

    public HwYunMsgMaker(TMsg tMsg) {
        TMsgSms tMsgSms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        this.templateId = tMsgSms.getTemplateId();
        paramList = new ArrayList<>();
        for (TemplateData templateData : tMsgSms.getTemplateDataList()) {
            paramList.add(templateData.getValue());
        }
    }

    /**
     * 组织华为云短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    @Override
    public String[] makeMsg(String[] msgData) {

        VelocityContext velocityContext = getVelocityContext(msgData);
        for (int i = 0; i < paramList.size(); i++) {
            paramList.set(i, TemplateUtil.evaluate(paramList.get(i), velocityContext));
        }
        String[] paramArray = new String[paramList.size()];
        return paramList.toArray(paramArray);
    }
}

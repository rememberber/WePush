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
 * 网易云信模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
@Getter
public class NeteaseYunXinMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private List<String> paramList;

    public NeteaseYunXinMsgMaker(TMsg tMsg) {
        TMsgSms tMsgSms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        this.templateId = tMsgSms.getTemplateId();

        paramList = new ArrayList<>();

        for (TemplateData templateData : tMsgSms.getTemplateDataList()) {
            paramList.add(templateData.getValue());
        }
    }

    /**
     * 组织网易云信短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    @Override
    public List<String> makeMsg(String[] msgData) {

        VelocityContext velocityContext = getVelocityContext(msgData);
        List<String> evaluated = new ArrayList<>(paramList.size());
        for (String paramValue : paramList) {
            evaluated.add(TemplateUtil.evaluate(paramValue, velocityContext));
        }
        return evaluated;
    }
}

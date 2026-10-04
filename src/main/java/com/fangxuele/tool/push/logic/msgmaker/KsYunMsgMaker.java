package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

import java.util.LinkedHashMap;
import java.util.Map;

/** 金山云命名模板参数，支持导入数据变量。 */
public final class KsYunMsgMaker extends BaseMsgMaker implements IMsgMaker {
    private final TMsgSms sms;

    public KsYunMsgMaker(TMsg message) {
        sms = JSON.parseObject(message.getContent(), TMsgSms.class);
        if (sms == null || sms.getTemplateId() == null || sms.getTemplateId().isBlank()) {
            throw new IllegalArgumentException("短信模板 ID 不能为空");
        }
    }

    @Override
    public Map<String, String> makeMsg(String[] data) {
        Map<String, String> values = new LinkedHashMap<>();
        VelocityContext context = getVelocityContext(data);
        if (sms.getTemplateDataList() != null) {
            for (TemplateData parameter : sms.getTemplateDataList()) {
                if (parameter.getName() == null || parameter.getName().isBlank()
                        || values.containsKey(parameter.getName())) {
                    throw new IllegalArgumentException("模板参数名不能为空或重复");
                }
                values.put(parameter.getName(), TemplateUtil.evaluate(parameter.getValue(), context));
            }
        }
        return Map.of("TplId", sms.getTemplateId().trim(), "TplParams", JSON.toJSONString(values));
    }
}

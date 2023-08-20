package com.fangxuele.tool.push.logic.msgmaker;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.http.MethodType;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.bean.account.AliYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 阿里云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class AliyunMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private List<TemplateData> templateDataList;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);

    private AliYunAccountConfig aliYunAccountConfig;

    public AliyunMsgMaker(TMsg tMsg) {
        TMsgSms tMsgSms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        this.templateId = tMsgSms.getTemplateId();
        this.templateDataList = tMsgSms.getTemplateDataList();

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        aliYunAccountConfig = JSON.parseObject(accountConfig, AliYunAccountConfig.class);
    }

    /**
     * 组织阿里云短信消息
     *
     * @param msgData 消息信息
     * @return SendSmsRequest
     */
    @Override
    public SendSmsRequest makeMsg(String[] msgData) {
        SendSmsRequest request = new SendSmsRequest();
        //使用post提交
        request.setSysMethod(MethodType.POST);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(aliYunAccountConfig.getSign());

        // 模板参数
        Map<String, String> paramMap = new HashMap<>(10);
        VelocityContext velocityContext = getVelocityContext(msgData);

        for (TemplateData templateData : templateDataList) {
            paramMap.put(templateData.getName(), TemplateUtil.evaluate(templateData.getValue(), velocityContext));
        }

        request.setTemplateParam(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信模板ID，传入的模板必须是在阿里阿里云短信中的可用模板。示例：SMS_585014
        request.setTemplateCode(templateId);

        return request;
    }
}

package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.ZhenziYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.ZhenziYunMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 榛子云模板短信发送器
 * 接口文档：http://smsow.zhenzikj.com/doc/java_sdk_doc.html
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class ZhenziYunMsgSender implements IMsgSender {
    /**
     * 默认接口地址
     */
    private static final String DEFAULT_API_URL = "https://sms_developer.zhenzisms.com";

    private final OkHttpClient httpClient;

    private ZhenziYunMsgMaker zhenziYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private ZhenziYunAccountConfig zhenziYunAccountConfig;

    public ZhenziYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        zhenziYunMsgMaker = new ZhenziYunMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        zhenziYunAccountConfig = JSON.parseObject(accountConfig, ZhenziYunAccountConfig.class);

        httpClient = HttpClientRegistry.get(MessageTypeEnum.ZHENZI_YUN_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer accountId) {
        HttpClientRegistry.invalidate(MessageTypeEnum.ZHENZI_YUN_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.ZHENZI_YUN_CODE, accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            // 模板ID
            String templateId = zhenziYunMsgMaker.getTemplateId();
            // 模板参数，多个参数以半角分号分隔
            String templateParams = StringUtils.join(zhenziYunMsgMaker.makeMsg(msgData), ";");
            // 目标手机号
            String number = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                String apiUrl = zhenziYunAccountConfig.getApiUrl();
                if (StringUtils.isEmpty(apiUrl)) {
                    apiUrl = DEFAULT_API_URL;
                }

                Request request = new Request.Builder().url(apiUrl + "/sms/send.html")
                        .post(new FormBody.Builder()
                                .add("appId", zhenziYunAccountConfig.getAppId())
                                .add("appSecret", zhenziYunAccountConfig.getAppSecret())
                                .add("templateId", templateId)
                                .add("templateParams", templateParams)
                                .add("number", number)
                                .build())
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                JSONObject result = JSON.parseObject(responseBody);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.isSuccessful() && result != null && result.getIntValue("code") == 0) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(responseBody);
                    log.error(responseBody);
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }
}

package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.LuosimaoAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.LuosimaoMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * Luosimao短信发送器
 * 接口文档：https://luosimao.com/docs/api/
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class LuosimaoMsgSender implements IMsgSender {
    /**
     * 发送短信接口地址
     */
    private static final String SEND_URL = "https://sms-api.luosimao.com/v1/send.json";

    private final OkHttpClient httpClient;

    private LuosimaoMsgMaker luosimaoMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private LuosimaoAccountConfig luosimaoAccountConfig;

    public LuosimaoMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        luosimaoMsgMaker = new LuosimaoMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        luosimaoAccountConfig = JSON.parseObject(accountConfig, LuosimaoAccountConfig.class);

        httpClient = HttpClientRegistry.get(MessageTypeEnum.LUOSIMAO_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer accountId) {
        HttpClientRegistry.invalidate(MessageTypeEnum.LUOSIMAO_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.LUOSIMAO_CODE, accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            // 短信内容
            String message = luosimaoMsgMaker.makeMsg(msgData);
            // 目标手机号
            String mobile = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                Request request = new Request.Builder().url(SEND_URL)
                        .header("Authorization", Credentials.basic("api", luosimaoAccountConfig.getApiKey()))
                        .post(new FormBody.Builder().add("mobile", mobile).add("message", message).build())
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                JSONObject result = JSON.parseObject(responseBody);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.isSuccessful() && result != null && result.getIntValue("error") == 0) {
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

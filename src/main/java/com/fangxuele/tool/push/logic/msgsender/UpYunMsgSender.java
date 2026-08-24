package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.YunPianAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.UpYunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 又拍云模板短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class UpYunMsgSender implements IMsgSender {
    /**
     * 又拍云短信sender
     */
    private final OkHttpClient okHttpClient;

    private UpYunMsgMaker upYunMsgMaker;

    private static final String URL = "https://sms-api.upyun.com/api/messages";

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private YunPianAccountConfig yunPianAccountConfig;

    public UpYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        upYunMsgMaker = new UpYunMsgMaker(tMsg);
        okHttpClient = HttpClientRegistry.get(MessageTypeEnum.UP_YUN_CODE, tMsg.getAccountId());
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        yunPianAccountConfig = JSON.parseObject(accountConfig, YunPianAccountConfig.class);
    }

    public static void removeAccount(Integer account1Id) {
        HttpClientRegistry.invalidate(MessageTypeEnum.UP_YUN_CODE, account1Id);
        ProviderTrafficController.invalidate(MessageTypeEnum.UP_YUN_CODE, account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = upYunMsgMaker.getTemplateId();
            String[] params = upYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];

            Request.Builder requestBuilder = new Request.Builder();
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            formBodyBuilder.add("mobile", telNum);
            formBodyBuilder.add("template_id", templateId);
            formBodyBuilder.add("vars", String.join("|", params));
            RequestBody requestBody = formBodyBuilder.build();
            requestBuilder.url(URL).post(requestBody);
            requestBuilder.addHeader("Authorization", yunPianAccountConfig.getApiKey());
            Request request = requestBuilder.build();
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(okHttpClient, request);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.isSuccessful()) {
                    sendResult.setSuccess(true);
                    sendResult.setInfo(response.body());
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.body());
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

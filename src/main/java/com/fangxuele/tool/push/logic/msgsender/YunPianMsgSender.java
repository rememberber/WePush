package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.YunPianAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.YunPianMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 云片网短信发送器
 * 接口文档：https://www.yunpian.com/official/document/sms/zh_cn/domestic_single_send
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class YunPianMsgSender implements IMsgSender {
    /**
     * 单条发送接口地址
     */
    private static final String SEND_URL = "https://sms.yunpian.com/v2/sms/single_send.json";

    private final OkHttpClient httpClient;

    private YunPianMsgMaker yunPianMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private String apiKey;

    private static final Map<Integer, String> apiKeyMap = new ConcurrentHashMap<>();


    public YunPianMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        yunPianMsgMaker = new YunPianMsgMaker(tMsg);
        apiKey = getApiKey(tMsg.getAccountId());
        this.dryRun = dryRun;

        httpClient = HttpClientRegistry.get(MessageTypeEnum.YUN_PIAN_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer account1Id) {
        apiKeyMap.remove(account1Id);
        HttpClientRegistry.invalidate(MessageTypeEnum.YUN_PIAN_CODE, account1Id);
        ProviderTrafficController.invalidate(MessageTypeEnum.YUN_PIAN_CODE, account1Id);
    }


    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String text = yunPianMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                Request request = new Request.Builder().url(SEND_URL)
                        .post(new FormBody.Builder().add("apikey", apiKey).add("mobile", telNum).add("text", text).build())
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
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

    private String getApiKey(Integer accountId) {
        return apiKeyMap.computeIfAbsent(accountId, ignored -> {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            YunPianAccountConfig yunPianAccountConfig = JSON.parseObject(accountConfig, YunPianAccountConfig.class);
            return yunPianAccountConfig.getApiKey();
        });

    }
}

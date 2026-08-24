package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.QiniuYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.QiNiuYunMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 七牛云模板短信发送器
 * 接口文档：https://developer.qiniu.com/sms/5897/sms-api-send-message
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class QiNiuYunMsgSender implements IMsgSender {
    /**
     * 发送短信接口地址
     */
    private static final String SEND_URL = "https://sms.qiniuapi.com/v1/message";

    private final OkHttpClient httpClient;

    private QiNiuYunMsgMaker qiNiuYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static final Map<Integer, QiniuYunAccountConfig> accountConfigMap = new ConcurrentHashMap<>();

    private QiniuYunAccountConfig qiniuYunAccountConfig;

    public QiNiuYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        qiNiuYunMsgMaker = new QiNiuYunMsgMaker(tMsg);
        qiniuYunAccountConfig = getAccountConfig(tMsg.getAccountId());
        this.dryRun = dryRun;

        httpClient = HttpClientRegistry.get(MessageTypeEnum.QI_NIU_YUN_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer account1Id) {
        accountConfigMap.remove(account1Id);
        HttpClientRegistry.invalidate(MessageTypeEnum.QI_NIU_YUN_CODE, account1Id);
        ProviderTrafficController.invalidate(MessageTypeEnum.QI_NIU_YUN_CODE, account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = qiNiuYunMsgMaker.getTemplateId();
            Map<String, String> params = qiNiuYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                JSONArray mobilesJson = new JSONArray();
                mobilesJson.add(telNum);

                JSONObject requestJson = new JSONObject();
                requestJson.put("template_id", templateId);
                requestJson.put("mobiles", mobilesJson);
                if (params != null && !params.isEmpty()) {
                    requestJson.put("parameters", (JSONObject) JSON.toJSON(params));
                }
                String body = requestJson.toJSONString();

                // 七牛鉴权：Qiniu <AccessKey>:<urlsafeBase64(HMAC-SHA1(secretKey, signingStr))>
                String signingStr = "POST /v1/message\nHost: sms.qiniuapi.com\nContent-Type: application/json\n\n" + body;
                byte[] sign = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, qiniuYunAccountConfig.getSecretKey())
                        .hmac(signingStr.getBytes(Charset.forName("UTF-8")));
                String authorization = "Qiniu " + qiniuYunAccountConfig.getAccessKey() + ":" + Base64.encodeBase64URLSafeString(sign);

                Request request = new Request.Builder().url(SEND_URL)
                        .header("Authorization", authorization)
                        .post(RequestBody.create(body, MediaType.get("application/json")))
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.statusCode() == 200 && !responseBody.contains("error")) {
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

    private QiniuYunAccountConfig getAccountConfig(Integer accountId) {
        return accountConfigMap.computeIfAbsent(accountId, ignored -> {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            return JSON.parseObject(accountConfig, QiniuYunAccountConfig.class);
        });

    }
}

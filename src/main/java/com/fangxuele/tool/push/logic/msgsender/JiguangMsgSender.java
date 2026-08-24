package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.JiguangAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.JiguangMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * 极光模板短信发送器
 * 接口文档：https://docs.jiguang.cn/jsms/server/rest_api_jsms
 * </pre>
 */
@Slf4j
public class JiguangMsgSender implements IMsgSender {
    private static final String BATCH_URL = "https://api.sms.jpush.cn/v1/messages/batch";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int BATCH_SIZE = 500;

    private final OkHttpClient httpClient;
    private final JiguangMsgMaker jiguangMsgMaker;
    private final Integer dryRun;
    private final JiguangAccountConfig jiguangAccountConfig;

    private static final TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static final TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public JiguangMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        jiguangMsgMaker = new JiguangMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        jiguangAccountConfig = JSON.parseObject(tAccount.getAccountConfig(), JiguangAccountConfig.class);
        httpClient = HttpClientRegistry.get(MessageTypeEnum.JIGUANG_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer accountId) {
        HttpClientRegistry.invalidate(MessageTypeEnum.JIGUANG_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.JIGUANG_CODE, accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        return sendBatch(Collections.singletonList(msgData)).get(0);
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    @Override
    public int recommendedBatchSize() {
        return BATCH_SIZE;
    }

    @Override
    public List<SendResult> sendBatch(List<String[]> batch) {
        if (batch.isEmpty()) {
            return List.of();
        }
        if (Integer.valueOf(1).equals(dryRun)) {
            List<SendResult> results = new ArrayList<>(batch.size());
            for (int i = 0; i < batch.size(); i++) {
                results.add(successResult("极光短信批量校验通过"));
            }
            return results;
        }

        try {
            int templateId;
            try {
                templateId = Integer.parseInt(jiguangMsgMaker.getTemplateId().trim());
            } catch (NumberFormatException e) {
                return uniformFailure(batch.size(), "极光短信模板ID必须为数字：" + jiguangMsgMaker.getTemplateId(), null, null);
            }

            JSONArray recipients = new JSONArray();
            for (String[] msgData : batch) {
                JSONObject recipient = new JSONObject();
                recipient.put("mobile", msgData[0]);
                recipient.put("temp_para", (JSONObject) JSON.toJSON(jiguangMsgMaker.makeMsg(msgData)));
                recipients.add(recipient);
            }
            JSONObject requestJson = new JSONObject();
            requestJson.put("temp_id", templateId);
            requestJson.put("recipients", recipients);

            Request request = new Request.Builder().url(BATCH_URL)
                    .header("Authorization", Credentials.basic(
                            jiguangAccountConfig.getAppKey(), jiguangAccountConfig.getMasterSecret()))
                    .post(RequestBody.create(requestJson.toJSONString(), JSON_MEDIA_TYPE))
                    .build();
            OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
            if (!response.isSuccessful()) {
                log.error("极光短信批量接口返回异常: status={}, body={}", response.statusCode(), response.body());
                return uniformFailure(batch.size(), response.body(), response.statusCode(), response.retryAfterMillis());
            }

            JSONObject responseJson = StringUtils.isBlank(response.body()) ? null : JSON.parseObject(response.body());
            JSONArray returnedRecipients = responseJson == null ? null : responseJson.getJSONArray("recipients");
            if (returnedRecipients == null || returnedRecipients.size() != batch.size()) {
                return uniformFailure(batch.size(),
                        "极光短信批量结果数与请求数不一致：" + response.body(),
                        response.statusCode(), response.retryAfterMillis());
            }

            List<SendResult> results = new ArrayList<>(returnedRecipients.size());
            for (int i = 0; i < returnedRecipients.size(); i++) {
                JSONObject recipientResult = returnedRecipients.getJSONObject(i);
                String errorCode = recipientResult.getString("error_code");
                boolean success = StringUtils.isBlank(errorCode)
                        && StringUtils.isNotBlank(recipientResult.getString("msg_id"));
                SendResult result = success
                        ? successResult(recipientResult.toJSONString())
                        : failureResult(StringUtils.defaultIfBlank(recipientResult.getString("error_message"),
                        recipientResult.toJSONString()));
                result.setHttpStatus(response.statusCode());
                result.setRetryAfterMillis(response.retryAfterMillis());
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return uniformFailure(batch.size(), e.getMessage(), null, null);
        }
    }

    private static List<SendResult> uniformFailure(int count, String info, Integer httpStatus, Long retryAfterMillis) {
        List<SendResult> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SendResult result = failureResult(info);
            result.setHttpStatus(httpStatus);
            result.setRetryAfterMillis(retryAfterMillis);
            results.add(result);
        }
        return results;
    }

    private static SendResult successResult(String info) {
        SendResult result = new SendResult();
        result.setSuccess(true);
        result.setInfo(info);
        return result;
    }

    private static SendResult failureResult(String info) {
        SendResult result = new SendResult();
        result.setSuccess(false);
        result.setInfo(info);
        return result;
    }
}

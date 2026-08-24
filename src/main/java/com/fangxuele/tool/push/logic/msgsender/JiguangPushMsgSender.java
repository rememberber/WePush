package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.JiguangPushMsg;
import com.fangxuele.tool.push.bean.account.JiguangPushAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.JiguangPushMsgMaker;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 极光推送发送器
 * 接口文档：https://docs.jiguang.cn/jpush/server/push/rest_api_v3_push
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class JiguangPushMsgSender implements IMsgSender {
    /**
     * 推送接口地址
     */
    private static final String PUSH_URL = "https://api.jpush.cn/v3/push";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final int MAX_AUDIENCE_PER_REQUEST = 1000;

    private final OkHttpClient httpClient;

    private JiguangPushMsgMaker jiguangPushMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private JiguangPushAccountConfig jiguangPushAccountConfig;

    public JiguangPushMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        jiguangPushMsgMaker = new JiguangPushMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        jiguangPushAccountConfig = JSON.parseObject(accountConfig, JiguangPushAccountConfig.class);

        httpClient = HttpClientRegistry.get(MessageTypeEnum.JIGUANG_PUSH_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer account1Id) {

        HttpClientRegistry.invalidate(MessageTypeEnum.JIGUANG_PUSH_CODE, account1Id);
        ProviderTrafficController.invalidate(MessageTypeEnum.JIGUANG_PUSH_CODE, account1Id);
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
        return MAX_AUDIENCE_PER_REQUEST;
    }

    @Override
    public List<SendResult> sendBatch(List<String[]> batch) {
        if (batch.isEmpty()) {
            return List.of();
        }
        List<SendResult> results = new ArrayList<>(Collections.nCopies(batch.size(), null));
        if (Integer.valueOf(1).equals(dryRun)) {
            for (int i = 0; i < batch.size(); i++) {
                results.set(i, successResult("极光推送批量校验通过"));
            }
            return results;
        }

        Map<PayloadKey, List<IndexedAudience>> groups = new LinkedHashMap<>();
        for (int i = 0; i < batch.size(); i++) {
            String[] msgData = batch.get(i);
            try {
                JiguangPushMsg pushMsg = jiguangPushMsgMaker.makeMsg(msgData);
                PayloadKey key = new PayloadKey(pushMsg.getTitle(), pushMsg.getContent(),
                        pushMsg.getExtras() == null ? Map.of()
                                : Collections.unmodifiableMap(new LinkedHashMap<>(pushMsg.getExtras())));
                groups.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(new IndexedAudience(i, msgData[0]));
            } catch (Exception e) {
                results.set(i, failureResult(e.getMessage()));
            }
        }

        for (Map.Entry<PayloadKey, List<IndexedAudience>> entry : groups.entrySet()) {
            List<IndexedAudience> recipients = entry.getValue();
            SendResult groupResult = sendGroup(entry.getKey(), recipients);
            for (IndexedAudience recipient : recipients) {
                results.set(recipient.index(), copyResult(groupResult));
            }
        }
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) == null) {
                results.set(i, failureResult("极光推送未返回结果"));
            }
        }
        return results;
    }

    private SendResult sendGroup(PayloadKey payload, List<IndexedAudience> recipients) {
        try {
            List<String> audience = recipients.stream().map(IndexedAudience::audience).toList();
            JSONObject requestJson = buildRequestJson(payload, audience);
            Request request = new Request.Builder().url(PUSH_URL)
                    .header("Authorization", Credentials.basic(
                            jiguangPushAccountConfig.getAppKey(), jiguangPushAccountConfig.getMasterSecret()))
                    .post(RequestBody.create(requestJson.toJSONString(), JSON_MEDIA_TYPE))
                    .build();
            OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
            JSONObject result = StringUtils.isBlank(response.body()) ? null : JSON.parseObject(response.body());
            SendResult sendResult;
            if (response.statusCode() == 200 && result != null && result.containsKey("msg_id")) {
                sendResult = successResult(response.body());
            } else {
                sendResult = failureResult(response.body());
                log.error(response.body());
            }
            sendResult.setHttpStatus(response.statusCode());
            sendResult.setRetryAfterMillis(response.retryAfterMillis());
            return sendResult;
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return failureResult(e.getMessage());
        }
    }

    private JSONObject buildRequestJson(PayloadKey payload, List<String> audience) {
        JSONObject extrasJson = (JSONObject) JSON.toJSON(payload.extras());
        JSONObject androidJson = new JSONObject();
        androidJson.put("alert", payload.content());
        androidJson.put("title", payload.title());
        androidJson.put("extras", extrasJson);
        JSONObject iosJson = new JSONObject();
        iosJson.put("alert", payload.content());
        iosJson.put("sound", "default");
        iosJson.put("extras", extrasJson);
        JSONObject notificationJson = new JSONObject();
        notificationJson.put("android", androidJson);
        notificationJson.put("ios", iosJson);
        JSONObject audienceJson = new JSONObject();
        String audienceType = StringUtils.defaultIfEmpty(jiguangPushMsgMaker.getAudienceType(), "alias");
        audienceJson.put(audienceType, audience);
        JSONObject optionsJson = new JSONObject();
        optionsJson.put("apns_production", jiguangPushMsgMaker.isApnsProduction());
        JSONObject requestJson = new JSONObject();
        requestJson.put("platform", "all");
        requestJson.put("audience", audienceJson);
        requestJson.put("notification", notificationJson);
        requestJson.put("options", optionsJson);
        return requestJson;
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

    private static SendResult copyResult(SendResult source) {
        SendResult result = new SendResult();
        result.setSuccess(source.isSuccess());
        result.setInfo(source.getInfo());
        result.setHttpStatus(source.getHttpStatus());
        result.setRetryAfterMillis(source.getRetryAfterMillis());
        return result;
    }

    record PayloadKey(String title, String content, Map<String, String> extras) {
    }

    private record IndexedAudience(int index, String audience) {
    }
}

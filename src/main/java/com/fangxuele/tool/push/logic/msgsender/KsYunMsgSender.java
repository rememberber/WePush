package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.KsYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.KsYunMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** 金山云 SendSms，协议参照 com.ksyun:ksc-sdk-sms-java:0.1.8。 */
public final class KsYunMsgSender implements IMsgSender {
    private final KsYunAccountConfig account;
    private final KsYunMsgMaker maker;
    private final boolean dryRun;
    private final OkHttpClient client;
    private final String endpoint;

    public KsYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg message = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class).selectByPrimaryKey(msgId);
        account = JSON.parseObject(MybatisUtil.getSqlSession().getMapper(TAccountMapper.class)
                .selectByPrimaryKey(message.getAccountId()).getAccountConfig(), KsYunAccountConfig.class);
        maker = new KsYunMsgMaker(message);
        this.dryRun = Integer.valueOf(1).equals(dryRun);
        client = HttpClientRegistry.get(MessageTypeEnum.KS_YUN_CODE, message.getAccountId());
        endpoint = account.endpoint();
    }

    KsYunMsgSender(KsYunAccountConfig account, KsYunMsgMaker maker, boolean dryRun,
                   OkHttpClient client, String endpoint) {
        this.account = account;
        this.maker = maker;
        this.dryRun = dryRun;
        this.client = client;
        this.endpoint = endpoint;
    }

    public static void removeAccount(Integer accountId) {
        HttpClientRegistry.invalidate(MessageTypeEnum.KS_YUN_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.KS_YUN_CODE, accountId);
    }

    @Override
    public SendResult send(String[] data) {
        SendResult result = new SendResult();
        try {
            account.validate();
            if (data == null || data.length == 0 || data[0] == null || !data[0].matches("\\+?[0-9]{6,20}")) {
                throw new IllegalArgumentException("请输入单个有效手机号码");
            }
            Map<String, String> business = maker.makeMsg(data);
            if (dryRun) {
                result.setSuccess(true);
                result.setInfo("试运行：模板参数校验通过，未发送短信");
                return result;
            }
            TreeMap<String, String> parameters = new TreeMap<>(business);
            parameters.put("Mobile", data[0]);
            parameters.put("SignName", account.getSign());
            parameters.put("Accesskey", account.getAccessKeyId());
            parameters.put("Service", "ksms");
            parameters.put("Action", "SendSms");
            parameters.put("Version", "2019-05-01");
            parameters.put("Timestamp", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC).format(Instant.now()));
            parameters.put("SignatureVersion", "1.0");
            parameters.put("SignatureMethod", "HMAC-SHA256");
            parameters.put("Region", account.getRegion());
            parameters.put("Signature", signature(parameters, account.getAccessKeySecret()));
            FormBody.Builder body = new FormBody.Builder();
            parameters.forEach(body::add);
            var response = OkHttpRequestUtil.execute(client,
                    new Request.Builder().url(endpoint).post(body.build()).build());
            result.setHttpStatus(response.statusCode());
            result.setRetryAfterMillis(response.retryAfterMillis());
            JSONObject json = JSON.parseObject(response.body());
            result.setSuccess(response.isSuccessful() && json != null && json.get("Error") == null
                    && json.getString("Sid") != null && !json.getString("Sid").isBlank());
            result.setInfo(result.isSuccess() ? "Sid=" + json.getString("Sid")
                    : "金山云短信请求失败（HTTP " + response.statusCode() + "）：" + response.body());
        } catch (Exception problem) {
            result.setInfo("金山云短信发送失败：" + problem.getClass().getSimpleName());
            if (problem instanceof IllegalArgumentException) result.setInfo(problem.getMessage());
        }
        return result;
    }

    static String signature(Map<String, String> parameters, String secret) throws Exception {
        StringBuilder canonical = new StringBuilder();
        new TreeMap<>(parameters).forEach((key, value) -> {
            if (!canonical.isEmpty()) canonical.append('&');
            canonical.append(encode(key)).append('=').append(encode(value));
        });
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    @Override
    public SendResult asyncSend(String[] data) { return send(data); }
}

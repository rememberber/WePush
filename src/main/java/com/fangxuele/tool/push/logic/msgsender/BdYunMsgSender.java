package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.BdYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.BdYunMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 百度云模板短信发送器
 * 接口文档：https://cloud.baidu.com/doc/SMS/s/xjwvys1jq
 * 认证机制：https://cloud.baidu.com/doc/Reference/s/njwvz1yfu
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class BdYunMsgSender implements IMsgSender {
    /**
     * 短信接口路径
     */
    private static final String SEND_PATH = "/bce/v2/message";

    private final OkHttpClient httpClient;

    private BdYunMsgMaker bdYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static final Map<Integer, BdYunAccountConfig> accountConfigMap = new ConcurrentHashMap<>();

    private BdYunAccountConfig bdYunAccountConfig;

    public BdYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        bdYunMsgMaker = new BdYunMsgMaker(tMsg);
        bdYunAccountConfig = getAccountConfig(tMsg.getAccountId());
        this.dryRun = dryRun;

        httpClient = HttpClientRegistry.get(MessageTypeEnum.BD_YUN_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer accountId) {
        accountConfigMap.remove(accountId);
        HttpClientRegistry.invalidate(MessageTypeEnum.BD_YUN_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.BD_YUN_CODE, accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateCode = bdYunMsgMaker.getTemplateId();
            Map<String, String> params = bdYunMsgMaker.makeMsg(msgData);
            String phoneNumber = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                JSONObject requestJson = new JSONObject();
                // 发送使用签名的调用ID
                requestJson.put("invokeId", bdYunAccountConfig.getBdInvokeId());
                requestJson.put("phoneNumber", phoneNumber);
                requestJson.put("templateCode", templateCode);
                if (params != null && !params.isEmpty()) {
                    requestJson.put("contentVar", (JSONObject) JSON.toJSON(params));
                }
                String payload = requestJson.toJSONString();

                // 兼容账号配置中endpoint带或不带http(s)://前缀的情况
                String endPoint = bdYunAccountConfig.getBdEndPoint();
                String scheme = "https";
                String host = endPoint;
                if (endPoint.startsWith("http")) {
                    URI uri = URI.create(endPoint);
                    scheme = uri.getScheme();
                    host = uri.getHost();
                }

                // ***** bce-auth-v1 签名 *****
                String timestamp = utcTimestamp();
                String authStringPrefix = "bce-auth-v1/" + bdYunAccountConfig.getBdAccessKeyId() + "/" + timestamp + "/1800";

                String canonicalHeaders = "host:" + uriEncode(host, true);
                String canonicalRequest = "POST\n" + uriEncode(SEND_PATH, false) + "\n\n" + canonicalHeaders;

                byte[] signingKey = hmacSha256(bdYunAccountConfig.getBdSecretAccessKey(), authStringPrefix);
                String signature = bytesToHex(hmacSha256(signingKey, canonicalRequest));

                String authorization = authStringPrefix + "/host/" + signature;

                Request request = new Request.Builder().url(scheme + "://" + host + SEND_PATH)
                        .header("Host", host)
                        .header("x-bce-date", timestamp)
                        .header("Authorization", authorization)
                        .post(RequestBody.create(payload, MediaType.get("application/json; charset=utf-8")))
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.isSuccessful() && result != null && result.getBooleanValue("success")) {
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

    private BdYunAccountConfig getAccountConfig(Integer accountId) {
        return accountConfigMap.computeIfAbsent(accountId, ignored -> {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            return JSON.parseObject(accountConfig, BdYunAccountConfig.class);
        });
    }

    /**
     * UTC时间，格式：yyyy-MM-dd'T'HH:mm:ss'Z'
     */
    private static String utcTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return df.format(new Date());
    }

    /**
     * BCE规范的URI编码：仅保留 A-Za-z0-9 及 "-._~"，斜杠按参数决定是否编码
     */
    private static String uriEncode(String input, boolean encodeSlash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input.getBytes(Charset.forName("UTF-8"))) {
            char ch = (char) (b & 0xFF);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                    || ch == '-' || ch == '.' || ch == '_' || ch == '~') {
                sb.append(ch);
            } else if (ch == '/' && !encodeSlash) {
                sb.append(ch);
            } else {
                sb.append('%').append(Character.toUpperCase(Character.forDigit((ch >> 4) & 0xF, 16)))
                        .append(Character.toUpperCase(Character.forDigit(ch & 0xF, 16)));
            }
        }
        return sb.toString();
    }

    private static byte[] hmacSha256(String key, String data) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key.getBytes(Charset.forName("UTF-8")))
                .hmac(data.getBytes(Charset.forName("UTF-8")));
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmac(data.getBytes(Charset.forName("UTF-8")));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}

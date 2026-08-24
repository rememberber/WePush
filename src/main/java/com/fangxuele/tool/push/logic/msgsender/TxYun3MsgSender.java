package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.TxYun3AccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.TxYun3MsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 腾讯云3.0模板短信发送器
 * 接口文档：https://cloud.tencent.com/document/api/382/55981
 * 签名方法：https://cloud.tencent.com/document/api/382/52072
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2023/9/4.
 */
@Slf4j
public class TxYun3MsgSender implements IMsgSender {
    /**
     * 服务名与接口版本
     */
    private static final String SERVICE = "sms";
    private static final String VERSION = "2021-01-11";
    private static final String ACTION = "SendSms";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private final OkHttpClient httpClient;

    private TxYun3MsgMaker txYun3MsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static final Map<Integer, TxYun3AccountConfig> accountConfigMap = new ConcurrentHashMap<>();

    private TxYun3AccountConfig txYun3AccountConfig;


    public TxYun3MsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        txYun3MsgMaker = new TxYun3MsgMaker(tMsg);
        txYun3AccountConfig = getAccountConfig(tMsg.getAccountId());
        this.dryRun = dryRun;

        httpClient = HttpClientRegistry.get(MessageTypeEnum.TX_YUN_3_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer account1Id) {
        accountConfigMap.remove(account1Id);
        HttpClientRegistry.invalidate(MessageTypeEnum.TX_YUN_3_CODE, account1Id);
        ProviderTrafficController.invalidate(MessageTypeEnum.TX_YUN_3_CODE, account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = txYun3MsgMaker.getTemplateId();
            String smsSign = txYun3AccountConfig.getSign();
            String[] params = txYun3MsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                String endpoint = txYun3AccountConfig.getEndPoint();
                String region = txYun3AccountConfig.getRegion();

                JSONArray templateParamSetJson = new JSONArray();
                for (String param : params) {
                    templateParamSetJson.add(param);
                }
                JSONArray phoneNumberSetJson = new JSONArray();
                phoneNumberSetJson.add(telNum);

                JSONObject requestJson = new JSONObject();
                requestJson.put("SmsSdkAppId", txYun3AccountConfig.getSdkAppId());
                requestJson.put("SignName", smsSign);
                requestJson.put("TemplateId", templateId);
                requestJson.put("TemplateParamSet", templateParamSetJson);
                requestJson.put("PhoneNumberSet", phoneNumberSetJson);
                String payload = requestJson.toJSONString();

                // ***** 第一步：拼接规范请求串 *****
                String hashedRequestPayload = DigestUtils.sha256Hex(payload);
                String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n" + "host:" + endpoint + "\n";
                String signedHeaders = "content-type;host";
                String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;

                // ***** 第二步：拼接待签名字符串 *****
                long timestamp = System.currentTimeMillis() / 1000;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
                String date = sdf.format(new Date(timestamp * 1000));
                String credentialScope = date + "/" + SERVICE + "/tc3_request";
                String hashedCanonicalRequest = DigestUtils.sha256Hex(canonicalRequest);
                String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;

                // ***** 第三步：计算签名 *****
                byte[] secretDate = hmacSha256(("TC3" + txYun3AccountConfig.getSecretKey()).getBytes(Charset.forName("UTF-8")), date);
                byte[] secretService = hmacSha256(secretDate, SERVICE);
                byte[] secretSigning = hmacSha256(secretService, "tc3_request");
                String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

                // ***** 第四步：拼接 Authorization *****
                String authorization = ALGORITHM + " Credential=" + txYun3AccountConfig.getSecretId() + "/" + credentialScope
                        + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

                Request request = new Request.Builder().url("https://" + endpoint)
                        .header("Authorization", authorization)
                        .header("Host", endpoint)
                        .header("X-TC-Action", ACTION)
                        .header("X-TC-Timestamp", String.valueOf(timestamp))
                        .header("X-TC-Version", VERSION)
                        .header("X-TC-Region", region)
                        .post(RequestBody.create(payload, MediaType.get(CONTENT_TYPE)))
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
                JSONObject responseObj = result == null ? null : result.getJSONObject("Response");
                JSONArray sendStatusSet = responseObj == null ? null : responseObj.getJSONArray("SendStatusSet");
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.isSuccessful() && sendStatusSet != null && !sendStatusSet.isEmpty()
                        && "Ok".equals(sendStatusSet.getJSONObject(0).getString("Code"))) {
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

    private TxYun3AccountConfig getAccountConfig(Integer accountId) {
        return accountConfigMap.computeIfAbsent(accountId, ignored -> {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            return JSON.parseObject(accountConfig, TxYun3AccountConfig.class);
        });
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
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

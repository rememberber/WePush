package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.AliYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.AliyunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TreeMap;
import java.util.UUID;

/**
 * <pre>
 * 阿里云模板短信发送器
 * 接口文档：https://help.aliyun.com/document_detail/101414.html
 * 签名机制：https://help.aliyun.com/document_detail/66384.html
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class AliYunMsgSender implements IMsgSender {
    /**
     * 短信服务接入地址
     */
    private static final String ENDPOINT = "https://dysmsapi.aliyuncs.com";

    private CloseableHttpClient closeableHttpClient;

    private AliyunMsgMaker aliyunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, AliYunAccountConfig> accountConfigMap = new HashMap<>();

    private AliYunAccountConfig aliYunAccountConfig;

    public AliYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        aliyunMsgMaker = new AliyunMsgMaker(tMsg);
        aliYunAccountConfig = getAccountConfig(tMsg.getAccountId());
        this.dryRun = dryRun;

        closeableHttpClient = HttpClients.createDefault();
    }

    public static void removeAccount(Integer accountId) {
        accountConfigMap.remove(accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            Map<String, String> bizParams = aliyunMsgMaker.makeMsg(msgData);
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                // 公共参数+业务参数
                TreeMap<String, String> queryParams = new TreeMap<>();
                queryParams.put("Action", "SendSms");
                queryParams.put("Version", "2017-05-25");
                queryParams.put("Format", "JSON");
                queryParams.put("RegionId", "cn-hangzhou");
                queryParams.put("AccessKeyId", aliYunAccountConfig.getAccessKeyId());
                queryParams.put("SignatureMethod", "HMAC-SHA1");
                queryParams.put("Timestamp", iso8601UtcNow());
                queryParams.put("SignatureVersion", "1.0");
                queryParams.put("SignatureNonce", UUID.randomUUID().toString());
                queryParams.put("PhoneNumbers", msgData[0]);
                queryParams.putAll(bizParams);

                // 构造规范化请求字符串
                StringBuilder canonicalizedQueryString = new StringBuilder();
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    canonicalizedQueryString.append("&").append(percentEncode(entry.getKey()))
                            .append("=").append(percentEncode(entry.getValue()));
                }

                // 构造待签名字符串
                String stringToSign = "GET&" + percentEncode("/") + "&" + percentEncode(canonicalizedQueryString.substring(1));

                // 计算签名
                byte[] sign = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, aliYunAccountConfig.getAccessKeySecret() + "&")
                        .hmac(stringToSign.getBytes(Charset.forName("UTF-8")));
                String signature = Base64.encodeBase64String(sign);

                String url = ENDPOINT + "/?Signature=" + percentEncode(signature) + canonicalizedQueryString;

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("GET").setUri(url).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
                if (result != null && "OK".equals(result.getString("Code"))) {
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

    private AliYunAccountConfig getAccountConfig(Integer accountId) {
        if (accountConfigMap.containsKey(accountId)) {
            return accountConfigMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            AliYunAccountConfig aliYunAccountConfig = JSON.parseObject(accountConfig, AliYunAccountConfig.class);

            accountConfigMap.put(accountId, aliYunAccountConfig);
            return aliYunAccountConfig;
        }

    }

    /**
     * ISO8601规范的UTC时间，如：2020-01-01T12:00:00Z
     */
    private static String iso8601UtcNow() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(System.currentTimeMillis());
    }

    /**
     * POP签名规范的特殊URL编码
     */
    private static String percentEncode(String value) throws Exception {
        return value == null ? null : URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }
}

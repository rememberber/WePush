package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.HwYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.HwYunMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <pre>
 * 华为云模板短信发送器
 * 部分代码来源于官网文档示例
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class HwYunMsgSender implements IMsgSender {
    private final OkHttpClient httpClient;

    /**
     * 无需修改,用于格式化鉴权头域,给"X-WSSE"参数赋值
     */
    private static final String WSSE_HEADER_FORMAT = "UsernameToken Username=\"%s\",PasswordDigest=\"%s\",Nonce=\"%s\",Created=\"%s\"";
    /**
     * 无需修改,用于格式化鉴权头域,给"Authorization"参数赋值
     */
    private static final String AUTH_HEADER_VALUE = "WSSE realm=\"SDP\",profile=\"UsernameToken\",type=\"Appkey\"";

    private HwYunMsgMaker hwYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private HwYunAccountConfig hwYunAccountConfig;

    public HwYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        hwYunMsgMaker = new HwYunMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        hwYunAccountConfig = JSON.parseObject(accountConfig, HwYunAccountConfig.class);

        // 保留历史兼容行为：华为云部分私有接入点使用自签证书。
        httpClient = HttpClientRegistry.getInsecure(MessageTypeEnum.HW_YUN_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer accountId) {
        HttpClientRegistry.invalidate(MessageTypeEnum.HW_YUN_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.HW_YUN_CODE, accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            //APP接入地址+接口访问URI
            String url = hwYunAccountConfig.getAccessUrl();
            //APP_Key
            String appKey = hwYunAccountConfig.getAppKey();
            //APP_Secret
            String appSecret = hwYunAccountConfig.getAppSecret();
            //国内短信签名通道号或国际/港澳台短信通道号
            String sender = hwYunAccountConfig.getSenderCode();
            String signature = hwYunAccountConfig.getSignature();
            //模板ID
            String templateId = hwYunMsgMaker.getTemplateId();
            //模板变量
            String templateParas = JSONUtil.toJsonStr(hwYunMsgMaker.makeMsg(msgData));
            String receiver = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                //请求Body,不携带签名名称时,signature请填null
                String body = buildRequestBody(sender, receiver, templateId, templateParas, "", signature);
                if (null == body || body.isEmpty()) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo("body is null.");
                    log.error("body is null.");
                    return sendResult;
                }

                //请求Headers中的X-WSSE参数值
                String wsseHeader = buildWsseHeader(appKey, appSecret);
                if (null == wsseHeader || wsseHeader.isEmpty()) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo("wsse header is null.");
                    log.error("wsse header is null.");
                    return sendResult;
                }

                Request request = new Request.Builder().url(url)
                        .header("Authorization", AUTH_HEADER_VALUE)
                        .header("X-WSSE", wsseHeader)
                        .post(RequestBody.create(body, MediaType.get("application/x-www-form-urlencoded")))
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                sendResult.setSuccess(response.isSuccessful());
                sendResult.setInfo(response.body());
                if (!response.isSuccessful()) {
                    log.error(response.body());
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

    /**
     * 构造请求Body体
     *
     * @param sender
     * @param receiver
     * @param templateId
     * @param templateParas
     * @param statusCallbackUrl
     * @param signature         | 签名名称,使用国内短信通用模板时填写
     * @return
     */
    static String buildRequestBody(String sender, String receiver, String templateId, String templateParas,
                                   String statusCallbackUrl, String signature) {
        if (null == sender || null == receiver || null == templateId || sender.isEmpty() || receiver.isEmpty()
                || templateId.isEmpty()) {
            System.out.println("buildRequestBody(): sender, receiver or templateId is null.");
            return null;
        }
        List<String> keyValues = new ArrayList<>();

        keyValues.add(formParam("from", sender));
        keyValues.add(formParam("to", receiver));
        keyValues.add(formParam("templateId", templateId));
        if (null != templateParas && !templateParas.isEmpty()) {
            keyValues.add(formParam("templateParas", templateParas));
        }
        if (null != statusCallbackUrl && !statusCallbackUrl.isEmpty()) {
            keyValues.add(formParam("statusCallback", statusCallbackUrl));
        }
        if (null != signature && !signature.isEmpty()) {
            keyValues.add(formParam("signature", signature));
        }

        return String.join("&", keyValues);
    }

    private static String formParam(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 构造X-WSSE参数值
     *
     * @param appKey
     * @param appSecret
     * @return
     */
    static String buildWsseHeader(String appKey, String appSecret) {
        if (null == appKey || null == appSecret || appKey.isEmpty() || appSecret.isEmpty()) {
            System.out.println("buildWsseHeader(): appKey or appSecret is null.");
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String time = sdf.format(new Date()); //Created
        String nonce = UUID.randomUUID().toString().replace("-", ""); //Nonce

        byte[] passwordDigest = DigestUtils.sha256(nonce + time + appSecret);
        String hexDigest = Hex.encodeHexString(passwordDigest);

        //如果JDK版本是1.8,请加载原生Base64类,并使用如下代码
        String passwordDigestBase64Str = Base64.getEncoder().encodeToString(hexDigest.getBytes()); //PasswordDigest
        //如果JDK版本低于1.8,请加载三方库提供Base64类,并使用如下代码
        //String passwordDigestBase64Str = Base64.encodeBase64String(hexDigest.getBytes(Charset.forName("utf-8"))); //PasswordDigest
        //若passwordDigestBase64Str中包含换行符,请执行如下代码进行修正
        //passwordDigestBase64Str = passwordDigestBase64Str.replaceAll("[\\s*\t\n\r]", "");

        return String.format(WSSE_HEADER_FORMAT, appKey, passwordDigestBase64Str, nonce, time);
    }
}

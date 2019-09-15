package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.HwYunMsgMaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
    /**
     * CloseableHttpClient
     */
    public volatile static CloseableHttpClient closeableHttpClient;

    /**
     * 无需修改,用于格式化鉴权头域,给"X-WSSE"参数赋值
     */
    private static final String WSSE_HEADER_FORMAT = "UsernameToken Username=\"%s\",PasswordDigest=\"%s\",Nonce=\"%s\",Created=\"%s\"";
    /**
     * 无需修改,用于格式化鉴权头域,给"Authorization"参数赋值
     */
    private static final String AUTH_HEADER_VALUE = "WSSE realm=\"SDP\",profile=\"UsernameToken\",type=\"Appkey\"";

    private HwYunMsgMaker hwYunMsgMaker;

    public HwYunMsgSender() {
        hwYunMsgMaker = new HwYunMsgMaker();
        closeableHttpClient = getHttpClient();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            //APP接入地址+接口访问URI
            String url = App.config.getHwAccessUrl();
            //APP_Key
            String appKey = App.config.getHwAppKey();
            //APP_Secret
            String appSecret = App.config.getHwAppSecretPassword();
            //国内短信签名通道号或国际/港澳台短信通道号
            String sender = App.config.getHwSenderCode();
            String signature = App.config.getHwSignature();
            //模板ID
            String templateId = HwYunMsgMaker.templateId;
            //模板变量
            String templateParas = JSONUtil.toJsonStr(hwYunMsgMaker.makeMsg(msgData));
            String receiver = msgData[0];
            if (PushControl.dryRun) {
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

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(url)
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                        .addHeader(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .addHeader("X-WSSE", wsseHeader)
                        .setEntity(new StringEntity(body)).build());

//                System.out.println(response.toString()); //打印响应头域信息
//                System.out.println(EntityUtils.toString(response.getEntity())); //打印响应消息实体
//                if (result.result == 0) {
                sendResult.setSuccess(true);
//                } else {
//                    sendResult.setSuccess(false);
//                    sendResult.setInfo(result.toString());
//                }
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
     * 获取CloseableHttpClient
     *
     * @return CloseableHttpClient
     */
    private static CloseableHttpClient getHttpClient() {
        if (closeableHttpClient == null) {
            synchronized (HwYunMsgSender.class) {
                if (closeableHttpClient == null) {
                    try {
                        // 为防止因HTTPS证书认证失败造成API调用失败,需要先忽略证书信任问题
                        closeableHttpClient = HttpClients.custom()
                                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null,
                                        (x509CertChain, authType) -> true).build())
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .build();
                    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return closeableHttpClient;
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
        List<NameValuePair> keyValues = new ArrayList<>();

        keyValues.add(new BasicNameValuePair("from", sender));
        keyValues.add(new BasicNameValuePair("to", receiver));
        keyValues.add(new BasicNameValuePair("templateId", templateId));
        if (null != templateParas && !templateParas.isEmpty()) {
            keyValues.add(new BasicNameValuePair("templateParas", templateParas));
        }
        if (null != statusCallbackUrl && !statusCallbackUrl.isEmpty()) {
            keyValues.add(new BasicNameValuePair("statusCallback", statusCallbackUrl));
        }
        if (null != signature && !signature.isEmpty()) {
            keyValues.add(new BasicNameValuePair("signature", signature));
        }

        return URLEncodedUtils.format(keyValues, Charset.forName("UTF-8"));
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

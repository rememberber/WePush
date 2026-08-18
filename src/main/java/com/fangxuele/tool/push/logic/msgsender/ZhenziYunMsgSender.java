package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.ZhenziYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.ZhenziYunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 榛子云模板短信发送器
 * 接口文档：http://smsow.zhenzikj.com/doc/java_sdk_doc.html
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class ZhenziYunMsgSender implements IMsgSender {
    /**
     * 默认接口地址
     */
    private static final String DEFAULT_API_URL = "https://sms_developer.zhenzisms.com";

    private CloseableHttpClient closeableHttpClient;

    private ZhenziYunMsgMaker zhenziYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private ZhenziYunAccountConfig zhenziYunAccountConfig;

    public ZhenziYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        zhenziYunMsgMaker = new ZhenziYunMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        zhenziYunAccountConfig = JSON.parseObject(accountConfig, ZhenziYunAccountConfig.class);

        closeableHttpClient = HttpClients.createDefault();
    }

    public static void removeAccount(Integer account1Id) {

        // do nothing
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            // 模板ID
            String templateId = zhenziYunMsgMaker.getTemplateId();
            // 模板参数，多个参数以半角分号分隔
            String templateParams = StringUtils.join(zhenziYunMsgMaker.makeMsg(msgData), ";");
            // 目标手机号
            String number = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                String apiUrl = zhenziYunAccountConfig.getApiUrl();
                if (StringUtils.isEmpty(apiUrl)) {
                    apiUrl = DEFAULT_API_URL;
                }

                List<NameValuePair> keyValues = new ArrayList<>();
                keyValues.add(new BasicNameValuePair("appId", zhenziYunAccountConfig.getAppId()));
                keyValues.add(new BasicNameValuePair("appSecret", zhenziYunAccountConfig.getAppSecret()));
                keyValues.add(new BasicNameValuePair("templateId", templateId));
                keyValues.add(new BasicNameValuePair("templateParams", templateParams));
                keyValues.add(new BasicNameValuePair("number", number));
                String body = URLEncodedUtils.format(keyValues, Charset.forName("UTF-8"));

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(apiUrl + "/sms/send.html")
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                        .setEntity(new StringEntity(body, Charset.forName("UTF-8"))).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject result = JSON.parseObject(responseBody);
                if (result != null && result.getIntValue("code") == 0) {
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
}

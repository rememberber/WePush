package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.LuosimaoAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.LuosimaoMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
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
 * Luosimao短信发送器
 * 接口文档：https://luosimao.com/docs/api/
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class LuosimaoMsgSender implements IMsgSender {
    /**
     * 发送短信接口地址
     */
    private static final String SEND_URL = "https://sms-api.luosimao.com/v1/send.json";

    private CloseableHttpClient closeableHttpClient;

    private LuosimaoMsgMaker luosimaoMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private LuosimaoAccountConfig luosimaoAccountConfig;

    public LuosimaoMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        luosimaoMsgMaker = new LuosimaoMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        luosimaoAccountConfig = JSON.parseObject(accountConfig, LuosimaoAccountConfig.class);

        closeableHttpClient = HttpClients.createDefault();
    }

    public static void removeAccount(Integer account1Id) {

        // do nothing
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            // 短信内容
            String message = luosimaoMsgMaker.makeMsg(msgData);
            // 目标手机号
            String mobile = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                List<NameValuePair> keyValues = new ArrayList<>();
                keyValues.add(new BasicNameValuePair("mobile", mobile));
                keyValues.add(new BasicNameValuePair("message", message));
                String body = URLEncodedUtils.format(keyValues, Charset.forName("UTF-8"));

                // HTTP Basic Auth，用户名固定为api，密码为API KEY
                String auth = "api:" + luosimaoAccountConfig.getApiKey();
                String authorization = "Basic " + Base64.encodeBase64String(auth.getBytes(Charset.forName("UTF-8")));

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(SEND_URL)
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                        .addHeader(HttpHeaders.AUTHORIZATION, authorization)
                        .setEntity(new StringEntity(body, Charset.forName("UTF-8"))).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject result = JSON.parseObject(responseBody);
                if (result != null && result.getIntValue("error") == 0) {
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

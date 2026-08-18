package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.YunPianAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.YunPianMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 云片网短信发送器
 * 接口文档：https://www.yunpian.com/official/document/sms/zh_cn/domestic_single_send
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class YunPianMsgSender implements IMsgSender {
    /**
     * 单条发送接口地址
     */
    private static final String SEND_URL = "https://sms.yunpian.com/v2/sms/single_send.json";

    private CloseableHttpClient closeableHttpClient;

    private YunPianMsgMaker yunPianMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private String apiKey;

    private static Map<Integer, String> apiKeyMap = new HashMap<>();


    public YunPianMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        yunPianMsgMaker = new YunPianMsgMaker(tMsg);
        apiKey = getApiKey(tMsg.getAccountId());
        this.dryRun = dryRun;

        closeableHttpClient = HttpClients.createDefault();
    }

    public static void removeAccount(Integer account1Id) {
        apiKeyMap.remove(account1Id);
    }


    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String text = yunPianMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                String body = "apikey=" + URLEncoder.encode(apiKey, "UTF-8")
                        + "&mobile=" + URLEncoder.encode(telNum, "UTF-8")
                        + "&text=" + URLEncoder.encode(text, "UTF-8");

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(SEND_URL)
                        .setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED.withCharset(Charset.forName("UTF-8")))).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
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

    private String getApiKey(Integer accountId) {
        if (apiKeyMap.containsKey(accountId)) {
            return apiKeyMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            YunPianAccountConfig yunPianAccountConfig = JSON.parseObject(accountConfig, YunPianAccountConfig.class);

            String apiKey = yunPianAccountConfig.getApiKey();

            apiKeyMap.put(accountId, apiKey);
            return apiKey;
        }

    }
}

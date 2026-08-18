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
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.Charset;

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

    private CloseableHttpClient closeableHttpClient;

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

        closeableHttpClient = HttpClients.createDefault();
    }

    public static void removeAccount(Integer account1Id) {

        // do nothing
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            JiguangPushMsg pushMsg = jiguangPushMsgMaker.makeMsg(msgData);
            // 目标标识（别名或RegistrationId）
            String audience = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                JSONObject extrasJson = (JSONObject) JSON.toJSON(pushMsg.getExtras());

                JSONObject androidJson = new JSONObject();
                androidJson.put("alert", pushMsg.getContent());
                androidJson.put("title", pushMsg.getTitle());
                androidJson.put("extras", extrasJson);

                JSONObject iosJson = new JSONObject();
                iosJson.put("alert", pushMsg.getContent());
                iosJson.put("sound", "default");
                iosJson.put("extras", extrasJson);

                JSONObject notificationJson = new JSONObject();
                notificationJson.put("android", androidJson);
                notificationJson.put("ios", iosJson);

                JSONObject audienceJson = new JSONObject();
                String audienceType = jiguangPushMsgMaker.getAudienceType();
                if (StringUtils.isEmpty(audienceType)) {
                    audienceType = "alias";
                }
                audienceJson.put(audienceType, new String[]{audience});

                JSONObject optionsJson = new JSONObject();
                optionsJson.put("apns_production", jiguangPushMsgMaker.isApnsProduction());

                JSONObject requestJson = new JSONObject();
                requestJson.put("platform", "all");
                requestJson.put("audience", audienceJson);
                requestJson.put("notification", notificationJson);
                requestJson.put("options", optionsJson);

                // HTTP Basic Auth，appKey:masterSecret
                String auth = jiguangPushAccountConfig.getAppKey() + ":" + jiguangPushAccountConfig.getMasterSecret();
                String authorization = "Basic " + Base64.encodeBase64String(auth.getBytes(Charset.forName("UTF-8")));

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(PUSH_URL)
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                        .addHeader(HttpHeaders.AUTHORIZATION, authorization)
                        .setEntity(new StringEntity(requestJson.toJSONString(), Charset.forName("UTF-8"))).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                int statusCode = response.getStatusLine().getStatusCode();
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
                if (statusCode == 200 && result != null && result.containsKey("msg_id")) {
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

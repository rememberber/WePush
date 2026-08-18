package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.JiguangAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.JiguangMsgMaker;
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
 * 极光模板短信发送器
 * 接口文档：https://docs.jiguang.cn/jsms/server/rest_api_jsms
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class JiguangMsgSender implements IMsgSender {
    /**
     * 发送模板短信接口地址
     */
    private static final String SEND_URL = "https://api.sms.jpush.cn/v1/messages";

    private CloseableHttpClient closeableHttpClient;

    private JiguangMsgMaker jiguangMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private JiguangAccountConfig jiguangAccountConfig;

    public JiguangMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        jiguangMsgMaker = new JiguangMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        jiguangAccountConfig = JSON.parseObject(accountConfig, JiguangAccountConfig.class);

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
            String templateId = jiguangMsgMaker.getTemplateId();
            // 模板变量键值对
            JSONObject tempPara = (JSONObject) JSON.toJSON(jiguangMsgMaker.makeMsg(msgData));
            // 目标手机号
            String mobile = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                JSONObject requestJson = new JSONObject();
                requestJson.put("mobile", mobile);
                try {
                    requestJson.put("temp_id", Integer.parseInt(templateId.trim()));
                } catch (NumberFormatException e) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo("极光短信模板ID必须为数字：" + templateId);
                    log.error(sendResult.getInfo());
                    return sendResult;
                }
                requestJson.put("temp_para", tempPara);

                // HTTP Basic Auth，appKey:masterSecret
                String auth = jiguangAccountConfig.getAppKey() + ":" + jiguangAccountConfig.getMasterSecret();
                String authorization = "Basic " + Base64.encodeBase64String(auth.getBytes(Charset.forName("UTF-8")));

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(SEND_URL)
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

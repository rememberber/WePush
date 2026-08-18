package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.TxYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.TxYunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <pre>
 * 腾讯云模板短信发送器
 * 接口文档：https://cloud.tencent.com/document/product/382/5976
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class TxYunMsgSender implements IMsgSender {
    /**
     * 单条发送接口地址
     */
    private static final String SEND_URL = "https://yun.tim.qq.com/v5/tlssmssvr/sendsms";

    private CloseableHttpClient closeableHttpClient;

    private TxYunMsgMaker txYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, TxYunAccountConfig> accountConfigMap = new HashMap<>();

    private TxYunAccountConfig txYunAccountConfig;


    public TxYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        txYunMsgMaker = new TxYunMsgMaker(tMsg);
        txYunAccountConfig = getAccountConfig(tMsg.getAccountId());
        this.dryRun = dryRun;

        closeableHttpClient = HttpClients.createDefault();
    }

    public static void removeAccount(Integer account1Id) {
        accountConfigMap.remove(account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            int templateId = txYunMsgMaker.getTemplateId();
            String smsSign = txYunAccountConfig.getSign();
            String[] params = txYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                long random = ThreadLocalRandom.current().nextInt(100000, 999999);
                long time = System.currentTimeMillis() / 1000;

                // sig = sha256("appkey=$appkey&random=$random&time=$time&mobile=$mobile")
                String sig = DigestUtils.sha256Hex("appkey=" + txYunAccountConfig.getAppKey()
                        + "&random=" + random + "&time=" + time + "&mobile=" + telNum);

                JSONObject telJson = new JSONObject();
                telJson.put("nationcode", "86");
                telJson.put("mobile", telNum);

                JSONArray paramsJson = new JSONArray();
                for (String param : params) {
                    paramsJson.add(param);
                }

                JSONObject requestJson = new JSONObject();
                requestJson.put("tel", telJson);
                requestJson.put("sign", smsSign);
                requestJson.put("tpl_id", templateId);
                requestJson.put("params", paramsJson);
                requestJson.put("sig", sig);
                requestJson.put("time", time);
                requestJson.put("extend", "");
                requestJson.put("ext", "");

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(SEND_URL + "?sdkappid=" + txYunAccountConfig.getAppId() + "&random=" + random)
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                        .setEntity(new StringEntity(requestJson.toJSONString(), Charset.forName("UTF-8"))).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject result = StringUtils.isBlank(responseBody) ? null : JSON.parseObject(responseBody);
                if (result != null && result.getIntValue("result") == 0) {
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

    public TxYunAccountConfig getAccountConfig(Integer accountId) {
        if (accountConfigMap.containsKey(accountId)) {
            return accountConfigMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            TxYunAccountConfig txYunAccountConfig = JSON.parseObject(accountConfig, TxYunAccountConfig.class);

            accountConfigMap.put(accountId, txYunAccountConfig);
            return txYunAccountConfig;
        }
    }
}

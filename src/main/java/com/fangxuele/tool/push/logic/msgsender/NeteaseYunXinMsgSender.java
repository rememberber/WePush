package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.NeteaseYunXinAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.NeteaseYunXinMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.UUID;

/**
 * <pre>
 * 网易云信模板短信发送器
 * 接口文档：https://doc.yunxin.163.com/sms/server-apis/jg2NDEyMzI?platform=server
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class NeteaseYunXinMsgSender implements IMsgSender {
    /**
     * 发送模板短信接口地址
     */
    private static final String SEND_TEMPLATE_URL = "https://api.netease.im/sms/sendtemplate.action";

    private CloseableHttpClient closeableHttpClient;

    private NeteaseYunXinMsgMaker neteaseYunXinMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private NeteaseYunXinAccountConfig neteaseYunXinAccountConfig;

    public NeteaseYunXinMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        neteaseYunXinMsgMaker = new NeteaseYunXinMsgMaker(tMsg);
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        neteaseYunXinAccountConfig = JSON.parseObject(accountConfig, NeteaseYunXinAccountConfig.class);

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
            String templateId = neteaseYunXinMsgMaker.getTemplateId();
            // 模板变量，按顺序的JSON数组
            String params = JSONUtil.toJsonStr(neteaseYunXinMsgMaker.makeMsg(msgData));
            // 目标手机号，JSON数组
            String mobiles = JSONUtil.toJsonStr(new String[]{msgData[0]});

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                List<NameValuePair> keyValues = new ArrayList<>();
                keyValues.add(new BasicNameValuePair("templateid", templateId));
                keyValues.add(new BasicNameValuePair("mobiles", mobiles));
                keyValues.add(new BasicNameValuePair("params", params));
                String body = URLEncodedUtils.format(keyValues, Charset.forName("UTF-8"));

                String appKey = neteaseYunXinAccountConfig.getAppKey();
                String appSecret = neteaseYunXinAccountConfig.getAppSecret();
                String nonce = UUID.randomUUID().toString().replace("-", "");
                String curTime = String.valueOf(System.currentTimeMillis() / 1000);
                // CheckSum = sha1(AppSecret + Nonce + CurTime)
                String checkSum = DigestUtils.sha1Hex(appSecret + nonce + curTime);

                HttpResponse response = closeableHttpClient.execute(RequestBuilder.create("POST")
                        .setUri(SEND_TEMPLATE_URL)
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                        .addHeader("AppKey", appKey)
                        .addHeader("Nonce", nonce)
                        .addHeader("CurTime", curTime)
                        .addHeader("CheckSum", checkSum)
                        .setEntity(new StringEntity(body, Charset.forName("UTF-8"))).build());

                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject result = JSON.parseObject(responseBody);
                if (result != null && result.getIntValue("code") == 200) {
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

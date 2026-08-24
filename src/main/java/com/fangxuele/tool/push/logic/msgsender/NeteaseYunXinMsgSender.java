package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.NeteaseYunXinAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.NeteaseYunXinMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

    private final OkHttpClient httpClient;

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

        httpClient = HttpClientRegistry.get(MessageTypeEnum.NETEASE_YUN_XIN_CODE, tMsg.getAccountId());
    }

    public static void removeAccount(Integer accountId) {
        HttpClientRegistry.invalidate(MessageTypeEnum.NETEASE_YUN_XIN_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.NETEASE_YUN_XIN_CODE, accountId);
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
                String appKey = neteaseYunXinAccountConfig.getAppKey();
                String appSecret = neteaseYunXinAccountConfig.getAppSecret();
                String nonce = UUID.randomUUID().toString().replace("-", "");
                String curTime = String.valueOf(System.currentTimeMillis() / 1000);
                // CheckSum = sha1(AppSecret + Nonce + CurTime)
                String checkSum = DigestUtils.sha1Hex(appSecret + nonce + curTime);

                Request request = new Request.Builder().url(SEND_TEMPLATE_URL)
                        .header("AppKey", appKey)
                        .header("Nonce", nonce)
                        .header("CurTime", curTime)
                        .header("CheckSum", checkSum)
                        .post(new FormBody.Builder()
                                .add("templateid", templateId)
                                .add("mobiles", mobiles)
                                .add("params", params)
                                .build())
                        .build();
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
                String responseBody = response.body();
                JSONObject result = JSON.parseObject(responseBody);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (response.isSuccessful() && result != null && result.getIntValue("code") == 200) {
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

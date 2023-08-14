package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.http.HttpClientConfig;
import com.aliyuncs.profile.DefaultProfile;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.AliYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.AliyunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 阿里云模板短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class AliYunMsgSender implements IMsgSender {
    /**
     * 阿里云短信client
     */
    public volatile static IAcsClient iAcsClient;

    private AliyunMsgMaker aliyunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, IAcsClient> acsClientMap = new HashMap<>();

    public AliYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        aliyunMsgMaker = new AliyunMsgMaker(tMsg);
        iAcsClient = getAliyunIAcsClient(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            //初始化acsClient,暂不支持region化
            SendSmsRequest sendSmsRequest = aliyunMsgMaker.makeMsg(msgData);
            sendSmsRequest.setPhoneNumbers(msgData[0]);
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                SendSmsResponse response = iAcsClient.getAcsResponse(sendSmsRequest);
                if (response.getCode() != null && "OK".equals(response.getCode())) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.getMessage() + ";ErrorCode:" + response.getCode());
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
     * 获取阿里云短信发送客户端
     *
     * @return IAcsClient
     */
    private static IAcsClient getAliyunIAcsClient() {
        if (iAcsClient == null) {
            synchronized (AliYunMsgSender.class) {
                if (iAcsClient == null) {
                    String aliyunAccessKeyId = App.config.getAliyunAccessKeyId();
                    String aliyunAccessKeySecret = App.config.getAliyunAccessKeySecret();

                    // 创建DefaultAcsClient实例并初始化
                    DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKeyId, aliyunAccessKeySecret);

                    // 多个SDK client共享一个连接池，此处设置该连接池的参数，
                    // 比如每个host的最大连接数，超时时间等
                    HttpClientConfig clientConfig = HttpClientConfig.getDefault();
                    clientConfig.setMaxRequestsPerHost(App.config.getMaxThreads());
                    clientConfig.setConnectionTimeoutMillis(10000L);

                    profile.setHttpClientConfig(clientConfig);
                    iAcsClient = new DefaultAcsClient(profile);
                }
            }
        }
        return iAcsClient;
    }

    private IAcsClient getAliyunIAcsClient(Integer accountId) {
        if (acsClientMap.containsKey(accountId)) {
            return acsClientMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            AliYunAccountConfig aliYunAccountConfig = JSON.parseObject(accountConfig, AliYunAccountConfig.class);

            String aliyunAccessKeyId = aliYunAccountConfig.getAccessKeyId();
            String aliyunAccessKeySecret = aliYunAccountConfig.getAccessKeySecret();

            // 创建DefaultAcsClient实例并初始化
            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKeyId, aliyunAccessKeySecret);

            // 多个SDK client共享一个连接池，此处设置该连接池的参数，
            // 比如每个host的最大连接数，超时时间等
            HttpClientConfig clientConfig = HttpClientConfig.getDefault();
            clientConfig.setMaxRequestsPerHost(App.config.getMaxThreads());
            clientConfig.setConnectionTimeoutMillis(10000L);

            profile.setHttpClientConfig(clientConfig);
            iAcsClient = new DefaultAcsClient(profile);

            acsClientMap.put(accountId, iAcsClient);

            return iAcsClient;
        }

    }
}

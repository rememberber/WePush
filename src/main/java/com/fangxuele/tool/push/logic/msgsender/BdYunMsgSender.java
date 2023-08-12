package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.sms.SmsClient;
import com.baidubce.services.sms.SmsClientConfiguration;
import com.baidubce.services.sms.model.SendMessageV2Request;
import com.baidubce.services.sms.model.SendMessageV2Response;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.BdYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.BdYunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 百度云模板短信发送器
 * 部分代码来源于官网文档示例
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class BdYunMsgSender implements IMsgSender {
    /**
     * 百度云短信SmsClient
     */
    public volatile static SmsClient smsClient;

    private BdYunMsgMaker bdYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, SmsClient> smsClientMap = new HashMap<>();

    public BdYunMsgSender() {
//        bdYunMsgMaker = new BdYunMsgMaker();
        smsClient = getBdYunSmsClient();
    }

    public BdYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        bdYunMsgMaker = new BdYunMsgMaker(tMsg);
        smsClient = getBdYunSmsClient(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateCode = BdYunMsgMaker.templateId;
            Map<String, String> params = bdYunMsgMaker.makeMsg(msgData);
            String phoneNumber = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                // 定义请求参数
                // 发送使用签名的调用ID
                // TODO
                String invokeId = App.config.getBdInvokeId();

                //实例化请求对象
                SendMessageV2Request request = new SendMessageV2Request();
                request.withInvokeId(invokeId)
                        .withPhoneNumber(phoneNumber)
                        .withTemplateCode(templateCode)
                        .withContentVar(params);

                // 发送请求
                SendMessageV2Response response = smsClient.sendMessage(request);

                if (response != null && response.isSuccess()) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.getMessage());
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
     * 获取百度云短信发送客户端
     *
     * @return SmsClient
     */
    private static SmsClient getBdYunSmsClient() {
        if (smsClient == null) {
            synchronized (BdYunMsgSender.class) {
                if (smsClient == null) {
                    // SMS服务域名，可根据环境选择具体域名
                    String endPoint = App.config.getBdEndPoint();
                    // 发送账号安全认证的Access Key ID
                    String accessKeyId = App.config.getBdAccessKeyId();
                    // 发送账号安全认证的Secret Access Key
                    String secretAccessKy = App.config.getBdSecretAccessKey();

                    // ak、sk等config
                    SmsClientConfiguration config = new SmsClientConfiguration();
                    config.setCredentials(new DefaultBceCredentials(accessKeyId, secretAccessKy));
                    config.setEndpoint(endPoint);

                    // 实例化发送客户端
                    smsClient = new SmsClient(config);
                }
            }
        }
        return smsClient;
    }

    private SmsClient getBdYunSmsClient(Integer accountId) {
        if (smsClientMap.containsKey(accountId)) {
            return smsClientMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            BdYunAccountConfig bdYunAccountConfig = JSON.parseObject(accountConfig, BdYunAccountConfig.class);

            // SMS服务域名，可根据环境选择具体域名
            String endPoint = bdYunAccountConfig.getBdEndPoint();
            // 发送账号安全认证的Access Key ID
            String accessKeyId = bdYunAccountConfig.getBdAccessKeyId();
            // 发送账号安全认证的Secret Access Key
            String secretAccessKy = bdYunAccountConfig.getBdSecretAccessKey();

            // ak、sk等config
            SmsClientConfiguration config = new SmsClientConfiguration();
            config.setCredentials(new DefaultBceCredentials(accessKeyId, secretAccessKy));
            config.setEndpoint(endPoint);

            // 实例化发送客户端
            smsClient = new SmsClient(config);

            smsClientMap.put(accountId, smsClient);
            return smsClient;
        }
    }
}

package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.QiniuYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.QiNiuYunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.qiniu.http.Response;
import com.qiniu.sms.SmsManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 七牛云模板短信发送器
 * 部分代码来源于官网文档示例
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class QiNiuYunMsgSender implements IMsgSender {
    /**
     * 七牛云短信smsManager
     */
    public volatile static SmsManager smsManager;

    private QiNiuYunMsgMaker qiNiuYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, SmsManager> smsManagerMap = new HashMap<>();

    public QiNiuYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        qiNiuYunMsgMaker = new QiNiuYunMsgMaker(tMsg);
        smsManager = getSmsManager(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = qiNiuYunMsgMaker.getTemplateId();
            Map<String, String> params = qiNiuYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                Response resp = smsManager.sendMessage(templateId, new String[]{telNum}, params);

//                if (resp.statusCode == 200) {
                sendResult.setSuccess(true);
//                } else {
//                    sendResult.setSuccess(false);
//                    sendResult.setInfo(resp.error);
//                }
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
     * 获取七牛云短信发送客户端
     *
     * @return SmsSingleSender
     */
    private static SmsManager getSmsManager() {
        if (smsManager == null) {
            synchronized (QiNiuYunMsgSender.class) {
                if (smsManager == null) {
                    // 设置需要操作的账号的AK和SK
                    String qiniuAccessKey = App.config.getQiniuAccessKey();
                    String qiniuSecretKey = App.config.getQiniuSecretKey();
                    Auth auth = Auth.create(qiniuAccessKey, qiniuSecretKey);

                    smsManager = new SmsManager(auth);
                }
            }
        }
        return smsManager;
    }


    private SmsManager getSmsManager(Integer accountId) {
        if (smsManagerMap.containsKey(accountId)) {
            return smsManagerMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            QiniuYunAccountConfig qiniuYunAccountConfig = JSON.parseObject(accountConfig, QiniuYunAccountConfig.class);

            // 设置需要操作的账号的AK和SK
            String qiniuAccessKey = qiniuYunAccountConfig.getAccessKey();
            String qiniuSecretKey = qiniuYunAccountConfig.getSecretKey();
            Auth auth = Auth.create(qiniuAccessKey, qiniuSecretKey);

            smsManager = new SmsManager(auth);

            smsManagerMap.put(accountId, smsManager);
            return smsManager;
        }

    }
}

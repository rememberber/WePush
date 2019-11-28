package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.QiNiuYunMsgMaker;
import com.qiniu.http.Response;
import com.qiniu.sms.SmsManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

    public QiNiuYunMsgSender() {
        qiNiuYunMsgMaker = new QiNiuYunMsgMaker();
        smsManager = getSmsManager();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = QiNiuYunMsgMaker.templateId;
            Map<String, String> params = qiNiuYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];

            if (PushControl.dryRun) {
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
}

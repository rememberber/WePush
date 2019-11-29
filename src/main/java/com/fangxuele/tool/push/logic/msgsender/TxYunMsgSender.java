package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.TxYunMsgMaker;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 腾讯云模板短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class TxYunMsgSender implements IMsgSender {
    /**
     * 腾讯云短信sender
     */
    public volatile static SmsSingleSender smsSingleSender;

    private TxYunMsgMaker txYunMsgMaker;

    public TxYunMsgSender() {
        txYunMsgMaker = new TxYunMsgMaker();
        smsSingleSender = getTxYunSender();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            int templateId = TxYunMsgMaker.templateId;
            String smsSign = App.config.getTxyunSign();
            String[] params = txYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                SmsSingleSenderResult result = smsSingleSender.sendWithParam("86", telNum,
                        templateId, params, smsSign, "", "");

                if (result.result == 0) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(result.toString());
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
     * 获取腾讯云短信发送客户端
     *
     * @return SmsSingleSender
     */
    private static SmsSingleSender getTxYunSender() {
        if (smsSingleSender == null) {
            synchronized (TxYunMsgSender.class) {
                if (smsSingleSender == null) {
                    String txyunAppId = App.config.getTxyunAppId();
                    String txyunAppKey = App.config.getTxyunAppKey();

                    smsSingleSender = new SmsSingleSender(Integer.parseInt(txyunAppId), txyunAppKey);
                }
            }
        }
        return smsSingleSender;
    }
}

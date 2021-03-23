package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxMpSubscribeMsgMaker;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.subscribe.WxMpSubscribeMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 微信公众号订阅通知发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/3/23.
 */
@Slf4j
public class WxMpSubscribeMsgSender implements IMsgSender {
    public volatile static WxMpService wxMpService;
    private WxMpSubscribeMsgMaker wxMpSubscribeMsgMaker;

    public WxMpSubscribeMsgSender() {
        wxMpSubscribeMsgMaker = new WxMpSubscribeMsgMaker();
        wxMpService = WxMpTemplateMsgSender.getWxMpService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMpSubscribeMessage wxMessageTemplate = wxMpSubscribeMsgMaker.makeMsg(msgData);
            wxMessageTemplate.setToUser(openId);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMpService.getSubscribeMsgService().send(wxMessageTemplate);
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }
}

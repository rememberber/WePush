package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxKefuMsgMaker;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 微信客服消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class WxKefuMsgSender implements IMsgSender {
    private WxKefuMsgMaker wxKefuMsgMaker;
    public volatile static WxMpService wxMpService;

    public WxKefuMsgSender() {
        wxKefuMsgMaker = new WxKefuMsgMaker();
        wxMpService = WxMpTemplateMsgSender.getWxMpService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMpKefuMessage wxMpKefuMessage = wxKefuMsgMaker.makeMsg(msgData);
            wxMpKefuMessage.setToUser(openId);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
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

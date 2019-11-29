package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxMaSubscribeMsgMaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 微信小程序订阅消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/29.
 */
@Slf4j
public class WxMaSubscribeMsgSender implements IMsgSender {
    public volatile static WxMaService wxMaService;

    private WxMaSubscribeMsgMaker wxMaSubscribeMsgMaker;

    public WxMaSubscribeMsgSender() {
        wxMaSubscribeMsgMaker = new WxMaSubscribeMsgMaker();
        wxMaService = WxMaTemplateMsgSender.getWxMaService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMaSubscribeMessage wxMaSubscribeMessage = wxMaSubscribeMsgMaker.makeMsg(msgData);
            wxMaSubscribeMessage.setToUser(openId);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMaService.getMsgService().sendSubscribeMsg(wxMaSubscribeMessage);
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

package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.logic.msgmaker.WxKefuMsgMaker;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;

/**
 * <pre>
 * 微信客服消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
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

        String openId = msgData[0];
        WxMpKefuMessage wxMpKefuMessage = wxKefuMsgMaker.makeMsg(msgData);
        wxMpKefuMessage.setToUser(openId);
        try {
            wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.toString());
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }
}

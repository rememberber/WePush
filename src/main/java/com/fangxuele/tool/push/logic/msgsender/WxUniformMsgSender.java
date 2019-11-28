package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import cn.binarywang.wx.miniapp.bean.WxMaUniformMessage;
import cn.binarywang.wx.miniapp.bean.WxMaUniformMessage.MiniProgram;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxMaTemplateMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 微信统一服务消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/29.
 */
@Slf4j
public class WxUniformMsgSender implements IMsgSender {

    private WxMpTemplateMsgMaker wxMpTemplateMsgMaker;

    private WxMaTemplateMsgMaker wxMaTemplateMsgMaker;

    public WxUniformMsgSender() {
        wxMpTemplateMsgMaker = new WxMpTemplateMsgMaker();
        wxMaTemplateMsgMaker = new WxMaTemplateMsgMaker();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMaTemplateMessage wxMaTemplateMessage = wxMaTemplateMsgMaker.makeMsg(msgData);
            WxMpTemplateMessage wxMpTemplateMessage = wxMpTemplateMsgMaker.makeMsg(msgData);

            WxMaUniformMessage wxMaUniformMessage = new WxMaUniformMessage();
            wxMaUniformMessage.setMpTemplateMsg(true);
            wxMaUniformMessage.setToUser(openId);
            wxMaUniformMessage.setAppid(App.config.getMiniAppAppId());
            wxMaUniformMessage.setTemplateId(wxMpTemplateMessage.getTemplateId());
            wxMaUniformMessage.setUrl(wxMpTemplateMessage.getUrl());
            wxMaUniformMessage.setPage(wxMaTemplateMessage.getPage());
            wxMaUniformMessage.setFormId(msgData[1]);
            MiniProgram miniProgram = new MiniProgram();
            miniProgram.setAppid(App.config.getMiniAppAppId());
            miniProgram.setPagePath(wxMaTemplateMessage.getPage());

            wxMaUniformMessage.setMiniProgram(miniProgram);
            wxMaUniformMessage.setData(wxMaTemplateMessage.getData());
            wxMaUniformMessage.setEmphasisKeyword(wxMaTemplateMessage.getEmphasisKeyword());

            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                WxMaTemplateMsgSender.getWxMaService().getMsgService().sendUniformMsg(wxMaUniformMessage);
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

package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateData;
import cn.binarywang.wx.miniapp.bean.WxMaUniformMessage;
import cn.binarywang.wx.miniapp.bean.WxMaUniformMessage.MiniProgram;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxMaSubscribeMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

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

    private WxMaSubscribeMsgMaker wxMaSubscribeMsgMaker;

    public WxUniformMsgSender() {
        wxMpTemplateMsgMaker = new WxMpTemplateMsgMaker();
        wxMaSubscribeMsgMaker = new WxMaSubscribeMsgMaker();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMaSubscribeMessage wxMaSubscribeMessage = wxMaSubscribeMsgMaker.makeMsg(msgData);
            WxMpTemplateMessage wxMpTemplateMessage = wxMpTemplateMsgMaker.makeMsg(msgData);

            WxMaUniformMessage wxMaUniformMessage = new WxMaUniformMessage();
            wxMaUniformMessage.setMpTemplateMsg(true);
            wxMaUniformMessage.setToUser(openId);
            wxMaUniformMessage.setAppid(App.config.getMiniAppAppId());
            wxMaUniformMessage.setTemplateId(wxMpTemplateMessage.getTemplateId());
            wxMaUniformMessage.setUrl(wxMpTemplateMessage.getUrl());
            wxMaUniformMessage.setPage(wxMaSubscribeMessage.getPage());
            wxMaUniformMessage.setFormId(msgData[1]);
            MiniProgram miniProgram = new MiniProgram();
            miniProgram.setAppid(App.config.getMiniAppAppId());
            miniProgram.setPagePath(wxMaSubscribeMessage.getPage());

            wxMaUniformMessage.setMiniProgram(miniProgram);
            List<WxMaTemplateData> wxMaTemplateDataList = Lists.newArrayList();
            List<WxMaSubscribeMessage.MsgData> data = wxMaSubscribeMessage.getData();
            WxMaTemplateData wxMaTemplateData;
            for (WxMaSubscribeMessage.MsgData datum : data) {
                wxMaTemplateData = new WxMaTemplateData();
                wxMaTemplateData.setName(datum.getName());
                wxMaTemplateData.setValue(datum.getValue());
                wxMaTemplateDataList.add(wxMaTemplateData);
            }
            wxMaUniformMessage.setData(wxMaTemplateDataList);

            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                WxMaSubscribeMsgSender.getWxMaService().getMsgService().sendUniformMsg(wxMaUniformMessage);
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

package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.WxMpSubscribeMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
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

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    public WxMpSubscribeMsgSender() {
//        wxMpSubscribeMsgMaker = new WxMpSubscribeMsgMaker();
        wxMpService = WxMpTemplateMsgSender.getWxMpService();
    }

    public WxMpSubscribeMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        wxMpSubscribeMsgMaker = new WxMpSubscribeMsgMaker(tMsg);
        wxMpService = WxMpTemplateMsgSender.getWxMpService(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMpSubscribeMessage wxMessageTemplate = wxMpSubscribeMsgMaker.makeMsg(msgData);
            wxMessageTemplate.setToUser(openId);
            if (dryRun == 1) {
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

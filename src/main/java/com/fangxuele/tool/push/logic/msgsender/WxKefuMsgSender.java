package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.WxKefuMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
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

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    public WxKefuMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        wxKefuMsgMaker = new WxKefuMsgMaker(tMsg);
        wxMpService = WxMpTemplateMsgSender.getWxMpService(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMpKefuMessage wxMpKefuMessage = wxKefuMsgMaker.makeMsg(msgData);
            wxMpKefuMessage.setToUser(openId);
            if (dryRun == 1) {
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

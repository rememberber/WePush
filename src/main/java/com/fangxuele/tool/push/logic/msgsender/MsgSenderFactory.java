package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.util.MybatisUtil;

/**
 * <pre>
 * 消息发送器工厂类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
public class MsgSenderFactory {

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public static IMsgSender getMsgSender(Integer msgId, Integer dryRun) {
        IMsgSender iMsgSender = null;
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        switch (tMsg.getMsgType()) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                iMsgSender = new WxMpTemplateMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
                iMsgSender = new WxMaSubscribeMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.KEFU_CODE:
                iMsgSender = new WxKefuMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                iMsgSender = new WxKefuPriorMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE:
                iMsgSender = new WxUniformMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                iMsgSender = new AliYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                iMsgSender = new TxYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.TX_YUN_3_CODE:
                iMsgSender = new TxYun3MsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.HW_YUN_CODE:
                iMsgSender = new HwYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                iMsgSender = new YunPianMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.EMAIL_CODE:
                iMsgSender = new MailMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.WX_CP_CODE:
                iMsgSender = new WxCpMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.HTTP_CODE:
                iMsgSender = new HttpMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.DING_CODE:
                iMsgSender = new DingMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.BD_YUN_CODE:
                iMsgSender = new BdYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.UP_YUN_CODE:
                iMsgSender = new UpYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.QI_NIU_YUN_CODE:
                iMsgSender = new QiNiuYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.MP_SUBSCRIBE_CODE:
                iMsgSender = new WxMpSubscribeMsgSender(msgId, dryRun);
                break;
            default:
                break;
        }
        return iMsgSender;
    }
}

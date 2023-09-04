package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;

/**
 * <pre>
 * 消息加工器工厂类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
public class MsgMakerFactory {

    /**
     * 获取消息加工器
     *
     * @return IMsgMaker
     */
    public static IMsgMaker getMsgMaker(TMsg tMsg) {
        IMsgMaker iMsgMaker = null;
        switch (App.config.getMsgType()) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                iMsgMaker = new WxMpTemplateMsgMaker(tMsg);
                break;
            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
                iMsgMaker = new WxMaSubscribeMsgMaker(tMsg);
                break;
            case MessageTypeEnum.KEFU_CODE:
                iMsgMaker = new WxKefuMsgMaker(tMsg);
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                iMsgMaker = new AliyunMsgMaker(tMsg);
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                iMsgMaker = new TxYunMsgMaker(tMsg);
                break;
            case MessageTypeEnum.TX_YUN_3_CODE:
                iMsgMaker = new TxYun3MsgMaker(tMsg);
                break;
            case MessageTypeEnum.HW_YUN_CODE:
                iMsgMaker = new HwYunMsgMaker(tMsg);
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                iMsgMaker = new YunPianMsgMaker(tMsg);
                break;
            case MessageTypeEnum.EMAIL_CODE:
                iMsgMaker = new MailMsgMaker(tMsg);
                break;
            case MessageTypeEnum.WX_CP_CODE:
                iMsgMaker = new WxCpMsgMaker(tMsg);
                break;
            case MessageTypeEnum.HTTP_CODE:
                iMsgMaker = new HttpMsgMaker(tMsg);
                break;
            case MessageTypeEnum.DING_CODE:
                iMsgMaker = new DingMsgMaker(tMsg);
                break;
            case MessageTypeEnum.BD_YUN_CODE:
                iMsgMaker = new BdYunMsgMaker(tMsg);
                break;
            case MessageTypeEnum.UP_YUN_CODE:
                iMsgMaker = new UpYunMsgMaker(tMsg);
                break;
            case MessageTypeEnum.QI_NIU_YUN_CODE:
                iMsgMaker = new QiNiuYunMsgMaker(tMsg);
                break;
            case MessageTypeEnum.MP_SUBSCRIBE_CODE:
                iMsgMaker = new WxMpSubscribeMsgMaker(tMsg);
                break;
            default:
        }
        return iMsgMaker;
    }
}

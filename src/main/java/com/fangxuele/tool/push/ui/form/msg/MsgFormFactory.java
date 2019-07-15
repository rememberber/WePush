package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;

/**
 * <pre>
 * 消息编辑界面工厂类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/15.
 */
public class MsgFormFactory {
    /**
     * 获取消息编辑界面
     *
     * @return IMsgForm
     */
    public static IMsgForm getMsgForm() {
        IMsgForm iMsgForm = null;
        switch (App.config.getMsgType()) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                iMsgForm = MpTemplateMsgForm.mpTemplateMsgForm;
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                iMsgForm = MaTemplateMsgForm.maTemplateMsgForm;
                break;
            case MessageTypeEnum.KEFU_CODE:
                iMsgForm = KefuMsgForm.kefuMsgForm;
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                iMsgForm = KefuPriorityMsgForm.kefuPriorityMsgForm;
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                iMsgForm = AliYunMsgForm.aliYunMsgForm;
                break;
            case MessageTypeEnum.ALI_TEMPLATE_CODE:
                iMsgForm = AliTemplateMsgForm.aliTemplateMsgForm;
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                iMsgForm = TxYunMsgForm.txYunMsgForm;
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                iMsgForm = YunpianMsgForm.yunpianMsgForm;
                break;
            case MessageTypeEnum.EMAIL_CODE:
                iMsgForm = MailMsgForm.mailMsgForm;
                break;
            case MessageTypeEnum.WX_CP_CODE:
                iMsgForm = WxCpMsgForm.wxCpMsgForm;
                break;
            case MessageTypeEnum.HTTP_CODE:
                iMsgForm = HttpMsgForm.httpMsgForm;
                break;
            default:
        }
        return iMsgForm;
    }
}

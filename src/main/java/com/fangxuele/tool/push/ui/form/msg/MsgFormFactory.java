package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.util.UndoUtil;

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
                iMsgForm = MpTemplateMsgForm.getInstance();
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
                iMsgForm = MaSubscribeMsgForm.getInstance();
                break;
            case MessageTypeEnum.KEFU_CODE:
                iMsgForm = KefuMsgForm.getInstance();
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                iMsgForm = KefuPriorityMsgForm.getInstance();
                UndoUtil.register(KefuMsgForm.getInstance());
                UndoUtil.register(MpTemplateMsgForm.getInstance());
                break;
            case MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE:
                iMsgForm = WxUniformMsgForm.getInstance();
                UndoUtil.register(MaSubscribeMsgForm.getInstance());
                UndoUtil.register(MpTemplateMsgForm.getInstance());
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                iMsgForm = AliYunMsgForm.getInstance();
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                iMsgForm = TxYunMsgForm.getInstance();
                break;
            case MessageTypeEnum.TX_YUN_3_CODE:
                iMsgForm = TxYun3MsgForm.getInstance();
                break;
            case MessageTypeEnum.QI_NIU_YUN_CODE:
                iMsgForm = QiNiuYunMsgForm.getInstance();
                break;
            case MessageTypeEnum.UP_YUN_CODE:
                iMsgForm = UpYunMsgForm.getInstance();
                break;
            case MessageTypeEnum.HW_YUN_CODE:
                iMsgForm = HwYunMsgForm.getInstance();
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                iMsgForm = YunpianMsgForm.getInstance();
                break;
            case MessageTypeEnum.EMAIL_CODE:
                iMsgForm = MailMsgForm.getInstance();
                break;
            case MessageTypeEnum.WX_CP_CODE:
                iMsgForm = WxCpMsgForm.getInstance();
                break;
            case MessageTypeEnum.DING_CODE:
                iMsgForm = DingMsgForm.getInstance();
                break;
            case MessageTypeEnum.BD_YUN_CODE:
                iMsgForm = BdYunMsgForm.getInstance();
                break;
            case MessageTypeEnum.MP_SUBSCRIBE_CODE:
                iMsgForm = MpSubscribeMsgForm.getInstance();
                break;
            case MessageTypeEnum.HTTP_CODE:
            default:
                iMsgForm = HttpMsgForm.getInstance();
        }
        if (iMsgForm != null) {
            UndoUtil.register(iMsgForm);
        }
        return iMsgForm;
    }
}

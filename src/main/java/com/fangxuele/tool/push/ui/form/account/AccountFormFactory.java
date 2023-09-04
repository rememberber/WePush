package com.fangxuele.tool.push.ui.form.account;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;

import javax.swing.*;

/**
 * <pre>
 * 账号编辑form界面工厂类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/3/11.
 */
public class AccountFormFactory {
    /**
     * 根据消息类型获取对应的账号编辑form界面Form
     *
     * @return IMsgSender
     */
    public static IAccountForm getAccountForm() {
        IAccountForm iAccountForm = null;
        switch (App.config.getMsgType()) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
            case MessageTypeEnum.MP_SUBSCRIBE_CODE:
            case MessageTypeEnum.KEFU_CODE:
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                iAccountForm = WxMpAccountForm.getInstance();
                break;
            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
            case MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE:
                iAccountForm = WxMaAccountForm.getInstance();
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                iAccountForm = AliYunAccountForm.getInstance();
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                iAccountForm = TxYunAccountForm.getInstance();
                break;
            case MessageTypeEnum.TX_YUN_3_CODE:
                iAccountForm = TxYun3AccountForm.getInstance();
                break;
            case MessageTypeEnum.HW_YUN_CODE:
                iAccountForm = HwYunAccountForm.getInstance();
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                iAccountForm = YunPianAccountForm.getInstance();
                break;
            case MessageTypeEnum.EMAIL_CODE:
                iAccountForm = EmailAccountForm.getInstance();
                break;
            case MessageTypeEnum.WX_CP_CODE:
                iAccountForm = WxCpAccountForm.getInstance();
                break;
            case MessageTypeEnum.HTTP_CODE:
                iAccountForm = HttpAccountForm.getInstance();
                break;
            case MessageTypeEnum.DING_CODE:
                iAccountForm = DingAccountForm.getInstance();
                break;
            case MessageTypeEnum.BD_YUN_CODE:
                iAccountForm = BdYunAccountForm.getInstance();
                break;
            case MessageTypeEnum.UP_YUN_CODE:
                iAccountForm = UpYunAccountForm.getInstance();
                break;
            case MessageTypeEnum.QI_NIU_YUN_CODE:
                iAccountForm = QiniuYunAccountForm.getInstance();
                break;
            default:
                break;
        }
        return iAccountForm;
    }

    /**
     * 根据消息类型获取对应的账号编辑form界面Panel
     *
     * @return
     */
    public static JPanel getAccountMainPanel() {
        return getAccountForm().getMainPanel();
    }

    /**
     * 根据消息类型获取对应的账号编辑form界面Panel并初始化
     *
     * @return
     */
    public static JPanel getAccountMainPanelAndInit(String accountName) {
        IAccountForm accountForm = getAccountForm();
        accountForm.clear();
        accountForm.init(accountName);
        return accountForm.getMainPanel();
    }
}

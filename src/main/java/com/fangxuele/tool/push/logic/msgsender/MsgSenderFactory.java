package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.util.HttpClientRegistry;
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
            case MessageTypeEnum.NETEASE_YUN_XIN_CODE:
                iMsgSender = new NeteaseYunXinMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.ZHENZI_YUN_CODE:
                iMsgSender = new ZhenziYunMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.LUOSIMAO_CODE:
                iMsgSender = new LuosimaoMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.JIGUANG_CODE:
                iMsgSender = new JiguangMsgSender(msgId, dryRun);
                break;
            case MessageTypeEnum.JIGUANG_PUSH_CODE:
                iMsgSender = new JiguangPushMsgSender(msgId, dryRun);
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
            case MessageTypeEnum.FEISHU_CODE:
                iMsgSender = new FeishuMsgSender(msgId, dryRun);
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
        if (iMsgSender == null) {
            return null;
        }
        return ProviderTrafficController.limit(iMsgSender, tMsg.getMsgType(), tMsg.getAccountId());
    }

    /** 账号更新或删除时统一释放通道缓存、连接池和并发闸门。 */
    public static void removeAccount(int msgType, int accountId) {
        if (MessageTypeEnum.isWxMpType(msgType)) {
            WxMpTemplateMsgSender.removeAccount(accountId);
        } else if (MessageTypeEnum.isWxMaType(msgType)) {
            WxMaSubscribeMsgSender.removeAccount(accountId);
        } else {
            switch (msgType) {
                case MessageTypeEnum.ALI_YUN_CODE -> AliYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.NETEASE_YUN_XIN_CODE -> NeteaseYunXinMsgSender.removeAccount(accountId);
                case MessageTypeEnum.TX_YUN_CODE -> TxYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.YUN_PIAN_CODE -> YunPianMsgSender.removeAccount(accountId);
                case MessageTypeEnum.UP_YUN_CODE -> UpYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.HW_YUN_CODE -> HwYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.EMAIL_CODE -> MailMsgSender.removeAccount(accountId);
                case MessageTypeEnum.WX_CP_CODE -> WxCpMsgSender.removeAccount(accountId);
                case MessageTypeEnum.HTTP_CODE -> HttpMsgSender.removeAccount(accountId);
                case MessageTypeEnum.DING_CODE -> DingMsgSender.removeAccount(accountId);
                case MessageTypeEnum.BD_YUN_CODE -> BdYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.QI_NIU_YUN_CODE -> QiNiuYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.TX_YUN_3_CODE -> TxYun3MsgSender.removeAccount(accountId);
                case MessageTypeEnum.ZHENZI_YUN_CODE -> ZhenziYunMsgSender.removeAccount(accountId);
                case MessageTypeEnum.LUOSIMAO_CODE -> LuosimaoMsgSender.removeAccount(accountId);
                case MessageTypeEnum.JIGUANG_CODE -> JiguangMsgSender.removeAccount(accountId);
                case MessageTypeEnum.JIGUANG_PUSH_CODE -> JiguangPushMsgSender.removeAccount(accountId);
                case MessageTypeEnum.FEISHU_CODE -> FeishuMsgSender.removeAccount(accountId);
                default -> {
                }
            }
        }
        // 未建立通道缓存或未知类型也要完成通用清理。
        HttpClientRegistry.invalidateAccount(accountId);
        ProviderTrafficController.invalidateAccount(accountId);
    }
}

package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.bean.DingMsg;
import com.fangxuele.tool.push.logic.msgsender.DingMsgSender;
import com.fangxuele.tool.push.ui.form.msg.DingMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

/**
 * <pre>
 * 钉钉消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/5.
 */
public class DingMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String agentId;

    public static String msgType;

    private static String msgTitle;

    private static String picUrl;

    public static String desc;

    public static String url;

    private static String btnTxt;

    private static String btnUrl;

    private static String msgContent;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        String agentIdBefore = agentId;
        String agentIdNow = DingMsgForm.appNameToAgentIdMap.get(DingMsgForm.getInstance().getAppNameComboBox().getSelectedItem());
        synchronized (this) {
            if (agentIdBefore == null || !agentIdBefore.equals(agentIdNow)) {
                agentId = agentIdNow;
                DingMsgSender.accessToken = null;
                DingMsgSender.defaultDingTalkClient = null;
            }
        }
        msgType = (String) DingMsgForm.getInstance().getMsgTypeComboBox().getSelectedItem();
        msgTitle = DingMsgForm.getInstance().getTitleTextField().getText();
        picUrl = DingMsgForm.getInstance().getPicUrlTextField().getText().trim();
        url = DingMsgForm.getInstance().getUrlTextField().getText().trim();
        btnTxt = DingMsgForm.getInstance().getBtnTxtTextField().getText().trim();
        btnUrl = DingMsgForm.getInstance().getBtnURLTextField().getText().trim();
        msgContent = DingMsgForm.getInstance().getContentTextArea().getText();
    }

    /**
     * 组织消息-企业号
     *
     * @param msgData 消息数据
     * @return WxMpTemplateMessage
     */
    @Override
    public DingMsg makeMsg(String[] msgData) {

        DingMsg dingMsg = new DingMsg();
        VelocityContext velocityContext = getVelocityContext(msgData);
        dingMsg.setContent(TemplateUtil.evaluate(msgContent, velocityContext));
        dingMsg.setTitle(TemplateUtil.evaluate(msgTitle, velocityContext));
        dingMsg.setPicUrl(TemplateUtil.evaluate(picUrl, velocityContext));
        dingMsg.setUrl(TemplateUtil.evaluate(url, velocityContext));
        dingMsg.setBtnTxt(TemplateUtil.evaluate(btnTxt, velocityContext));
        dingMsg.setBtnUrl(TemplateUtil.evaluate(btnUrl, velocityContext));

        return dingMsg;
    }
}

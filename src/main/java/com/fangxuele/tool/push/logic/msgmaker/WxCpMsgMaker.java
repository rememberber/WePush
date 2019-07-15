package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.logic.msgsender.WxCpMsgSender;
import com.fangxuele.tool.push.ui.form.msg.WxCpMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.bean.article.NewArticle;
import org.apache.velocity.VelocityContext;

/**
 * <pre>
 * 企业号消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/29.
 */
public class WxCpMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String agentId;

    public static String msgType;

    public static String msgTitle;

    public static String picUrl;

    public static String desc;

    public static String url;

    public static String btnTxt;

    public static String msgContent;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        agentId = WxCpMsgForm.appNameToAgentIdMap.get(WxCpMsgForm.wxCpMsgForm.getAppNameComboBox().getSelectedItem());
        msgType = (String) WxCpMsgForm.wxCpMsgForm.getMsgTypeComboBox().getSelectedItem();
        msgTitle = WxCpMsgForm.wxCpMsgForm.getTitleTextField().getText();
        picUrl = WxCpMsgForm.wxCpMsgForm.getPicUrlTextField().getText().trim();
        desc = WxCpMsgForm.wxCpMsgForm.getDescTextField().getText();
        url = WxCpMsgForm.wxCpMsgForm.getUrlTextField().getText().trim();
        btnTxt = WxCpMsgForm.wxCpMsgForm.getBtnTxtTextField().getText().trim();
        msgContent = WxCpMsgForm.wxCpMsgForm.getContentTextArea().getText();
        WxCpMsgSender.wxCpConfigStorage = null;
        WxCpMsgSender.wxCpService = null;
    }

    /**
     * 组织消息-企业号
     *
     * @param msgData 消息数据
     * @return WxMpTemplateMessage
     */
    @Override
    public WxCpMessage makeMsg(String[] msgData) {

        WxCpMessage wxCpMessage = null;
        VelocityContext velocityContext = getVelocityContext(msgData);
        if ("图文消息".equals(msgType)) {
            NewArticle article = new NewArticle();

            // 标题
            String title = TemplateUtil.evaluate(msgTitle, velocityContext);
            article.setTitle(title);

            // 图片url
            article.setPicUrl(picUrl);

            // 描述
            String description = TemplateUtil.evaluate(desc, velocityContext);
            article.setDescription(description);

            // 跳转url
            article.setUrl(url);

            wxCpMessage = WxCpMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(msgType)) {
            String content = TemplateUtil.evaluate(msgContent, velocityContext);
            wxCpMessage = WxCpMessage.TEXT().agentId(Integer.valueOf(agentId)).toUser(msgData[0]).content(content).build();
        } else if ("markdown消息".equals(msgType)) {
            String content = TemplateUtil.evaluate(msgContent, velocityContext);
            wxCpMessage = WxCpMessage.MARKDOWN().agentId(Integer.valueOf(agentId)).toUser(msgData[0]).content(content).build();
        } else if ("文本卡片消息".equals(msgType)) {
            // 标题
            String title = TemplateUtil.evaluate(msgTitle, velocityContext);
            // 描述
            String description = TemplateUtil.evaluate(desc, velocityContext);
            // 跳转url
            String urlLink = TemplateUtil.evaluate(url, velocityContext);
            wxCpMessage = WxCpMessage.TEXTCARD().agentId(Integer.valueOf(agentId)).toUser(msgData[0]).title(title)
                    .description(description).url(urlLink).btnTxt(btnTxt).build();
        }

        return wxCpMessage;
    }
}

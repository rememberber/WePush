package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.ui.form.msg.KefuMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import org.apache.velocity.VelocityContext;

/**
 * <pre>
 * 微信客服消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class WxKefuMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private static String msgKefuMsgType;

    private static String msgKefuMsgTitle;

    private static String msgKefuMsgContent;

    private static String msgKefuPicUrl;

    private static String msgKefuDesc;

    private static String msgKefuUrl;

    private static String msgKefuAppid;

    private static String msgKefuPagepath;

    private static String msgKefuThumbMediaId;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        msgKefuMsgType = KefuMsgForm.getInstance().getMsgKefuMsgTypeComboBox().getSelectedItem().toString();
        msgKefuMsgTitle = KefuMsgForm.getInstance().getMsgKefuMsgTitleTextField().getText();
        msgKefuPicUrl = KefuMsgForm.getInstance().getMsgKefuPicUrlTextField().getText();
        msgKefuDesc = KefuMsgForm.getInstance().getMsgKefuDescTextField().getText();
        msgKefuUrl = KefuMsgForm.getInstance().getMsgKefuUrlTextField().getText();
        msgKefuMsgContent = KefuMsgForm.getInstance().getContentTextArea().getText();
        msgKefuAppid = KefuMsgForm.getInstance().getMsgKefuAppidTextField().getText();
        msgKefuPagepath = KefuMsgForm.getInstance().getMsgKefuPagepathTextField().getText();
        msgKefuThumbMediaId = KefuMsgForm.getInstance().getMsgKefuThumbMediaIdTextField().getText();
    }

    /**
     * 组织客服消息
     *
     * @param msgData 消息信息
     * @return WxMpKefuMessage
     */
    @Override
    public WxMpKefuMessage makeMsg(String[] msgData) {

        WxMpKefuMessage kefuMessage = null;
        VelocityContext velocityContext = getVelocityContext(msgData);
        if ("图文消息".equals(msgKefuMsgType)) {
            WxMpKefuMessage.WxArticle article = new WxMpKefuMessage.WxArticle();

            // 标题
            String title = TemplateUtil.evaluate(msgKefuMsgTitle, velocityContext);
            article.setTitle(title);

            // 图片url
            article.setPicUrl(msgKefuPicUrl);

            // 描述
            String description = TemplateUtil.evaluate(msgKefuDesc, velocityContext);
            article.setDescription(description);

            // 跳转url
            String url = TemplateUtil.evaluate(msgKefuUrl, velocityContext);
            article.setUrl(url);

            kefuMessage = WxMpKefuMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(msgKefuMsgType)) {
            String content = TemplateUtil.evaluate(msgKefuMsgContent, velocityContext);
            kefuMessage = WxMpKefuMessage.TEXT().content(content).build();
        } else if ("小程序卡片消息".equals(msgKefuMsgType)) {
            String title = TemplateUtil.evaluate(msgKefuMsgTitle, velocityContext);
            String pagePath = TemplateUtil.evaluate(msgKefuPagepath, velocityContext);
            String thumbMediaId = TemplateUtil.evaluate(msgKefuThumbMediaId, velocityContext);
            kefuMessage = WxMpKefuMessage.MINIPROGRAMPAGE().title(title).appId(msgKefuAppid).pagePath(pagePath).thumbMediaId(thumbMediaId).build();
        }

        return kefuMessage;
    }
}

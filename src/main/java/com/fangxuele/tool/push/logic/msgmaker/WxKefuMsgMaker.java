package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgKefu;
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

    private String msgKefuMsgType;

    private String msgKefuMsgTitle;

    private String msgKefuMsgContent;

    private String msgKefuPicUrl;

    private String msgKefuDesc;

    private String msgKefuUrl;

    private String msgKefuAppid;

    private String msgKefuPagepath;

    private String msgKefuThumbMediaId;

    public WxKefuMsgMaker(TMsg tMsg) {
        TMsgKefu tMsgKefu = JSON.parseObject(tMsg.getContent(), TMsgKefu.class);

        msgKefuMsgType = tMsgKefu.getKefuMsgType();
        msgKefuMsgTitle = tMsgKefu.getTitle();
        msgKefuPicUrl = tMsgKefu.getImgUrl();
        msgKefuDesc = tMsgKefu.getDescribe();
        msgKefuUrl = tMsgKefu.getUrl();
        msgKefuMsgContent = tMsgKefu.getContent();
        msgKefuAppid = tMsgKefu.getAppId();
        msgKefuPagepath = tMsgKefu.getPagePath();
        msgKefuThumbMediaId = tMsgKefu.getThumbMediaId();
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

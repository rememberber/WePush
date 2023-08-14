package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgDing;
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

    private String msgType;

    private String msgTitle;

    private String picUrl;

    private String url;

    private String btnTxt;

    private String btnUrl;

    private String msgContent;

    private String radioType;

    private String webHook;

    public DingMsgMaker(TMsg tMsg) {
        TMsgDing tMsgDing = JSON.parseObject(tMsg.getContent(), TMsgDing.class);

        msgType = tMsgDing.getDingMsgType();
        msgTitle = tMsgDing.getMsgTitle();
        picUrl = tMsgDing.getPicUrl();
        url = tMsgDing.getUrl();
        btnTxt = tMsgDing.getBtnTxt();
        btnUrl = tMsgDing.getBtnUrl();
        msgContent = tMsgDing.getContent();
        radioType = tMsgDing.getRadioType();
        webHook = tMsgDing.getWebHook();
    }

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
    }

    /**
     * 组织消息-企业号
     *
     * @param msgData 消息数据
     * @return WxMpTemplateMessage
     */
    @Override
    public TMsgDing makeMsg(String[] msgData) {

        TMsgDing dingMsg = new TMsgDing();
        VelocityContext velocityContext = getVelocityContext(msgData);
        if ("markdown消息".equals(msgType)) {
            dingMsg.setContent(msgContent);
        } else {
            dingMsg.setContent(TemplateUtil.evaluate(msgContent, velocityContext));
        }
        dingMsg.setMsgTitle(TemplateUtil.evaluate(msgTitle, velocityContext));
        dingMsg.setPicUrl(TemplateUtil.evaluate(picUrl, velocityContext));
        dingMsg.setUrl(TemplateUtil.evaluate(url, velocityContext));
        dingMsg.setBtnTxt(TemplateUtil.evaluate(btnTxt, velocityContext));
        dingMsg.setBtnUrl(TemplateUtil.evaluate(btnUrl, velocityContext));

        return dingMsg;
    }
}

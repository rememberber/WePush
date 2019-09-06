package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response;
import com.fangxuele.tool.push.bean.DingMsg;
import com.fangxuele.tool.push.dao.TDingAppMapper;
import com.fangxuele.tool.push.domain.TDingApp;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.DingMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * <pre>
 * 钉钉消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/5.
 */
@Slf4j
public class DingMsgSender implements IMsgSender {
    public volatile static DefaultDingTalkClient defaultDingTalkClient;
    public static TimedCache<String, String> accessTokenTimedCache;
    private DingMsgMaker dingMsgMaker;

    private static TDingAppMapper dingAppMapper = MybatisUtil.getSqlSession().getMapper(TDingAppMapper.class);

    public DingMsgSender() {
        dingMsgMaker = new DingMsgMaker();
        defaultDingTalkClient = getDefaultDingTalkClient();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String userId = msgData[0];

            OapiMessageCorpconversationAsyncsendV2Request request2 = new OapiMessageCorpconversationAsyncsendV2Request();
            request2.setUseridList(userId);
            request2.setAgentId(Long.valueOf(DingMsgMaker.agentId));
            request2.setToAllUser(false);

            DingMsg dingMsg = dingMsgMaker.makeMsg(msgData);
            OapiMessageCorpconversationAsyncsendV2Request.Msg msg = getMsg(dingMsg);
            request2.setMsg(msg);

            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                OapiMessageCorpconversationAsyncsendV2Response response2 = defaultDingTalkClient.execute(request2, getAccessTokenTimedCache().get("accessToken"));
                if (response2.getErrcode() != 0) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response2.getErrmsg());
                    log.error(response2.getErrmsg());
                    return sendResult;
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(e.toString());
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }

    private OapiMessageCorpconversationAsyncsendV2Request.Msg getMsg(DingMsg dingMsg) {
        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        if ("文本消息".equals(DingMsgMaker.msgType)) {
            msg.setMsgtype("text");
            msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
            msg.getText().setContent(dingMsg.getContent());
        } else if ("链接消息".equals(DingMsgMaker.msgType)) {
            msg.setMsgtype("link");
            msg.setLink(new OapiMessageCorpconversationAsyncsendV2Request.Link());
            msg.getLink().setTitle(dingMsg.getTitle());
            msg.getLink().setText(dingMsg.getContent());
            msg.getLink().setMessageUrl(dingMsg.getUrl());
            msg.getLink().setPicUrl(dingMsg.getPicUrl());
        } else if ("markdown消息".equals(DingMsgMaker.msgType)) {
            msg.setMsgtype("markdown");
            msg.setMarkdown(new OapiMessageCorpconversationAsyncsendV2Request.Markdown());
            msg.getMarkdown().setText(dingMsg.getContent());
            msg.getMarkdown().setTitle(dingMsg.getTitle());
        } else if ("卡片消息".equals(DingMsgMaker.msgType)) {
            msg.setMsgtype("action_card");
            msg.setActionCard(new OapiMessageCorpconversationAsyncsendV2Request.ActionCard());
            msg.getActionCard().setTitle(dingMsg.getTitle());
            msg.getActionCard().setMarkdown(dingMsg.getContent());
            msg.getActionCard().setSingleTitle(dingMsg.getBtnTxt());
            msg.getActionCard().setSingleUrl(dingMsg.getBtnUrl());
        }
        return msg;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    /**
     * 获取微信企业号工具服务
     *
     * @return WxCpService
     */
    public static DefaultDingTalkClient getDefaultDingTalkClient() {
        if (defaultDingTalkClient == null) {
            synchronized (PushControl.class) {
                if (defaultDingTalkClient == null) {
                    defaultDingTalkClient = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");
                }
            }
        }
        return defaultDingTalkClient;
    }

    public static TimedCache<String, String> getAccessTokenTimedCache() {
        if (accessTokenTimedCache == null || StringUtils.isEmpty(accessTokenTimedCache.get("accessToken"))) {
            synchronized (PushControl.class) {
                if (accessTokenTimedCache == null || StringUtils.isEmpty(accessTokenTimedCache.get("accessToken"))) {
                    DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
                    OapiGettokenRequest request = new OapiGettokenRequest();
                    TDingApp tDingApp = dingAppMapper.selectByAgentId(DingMsgMaker.agentId);
                    request.setAppkey(tDingApp.getAppKey());
                    request.setAppsecret(tDingApp.getAppSecret());
                    request.setHttpMethod("GET");
                    OapiGettokenResponse response = null;
                    try {
                        response = client.execute(request);
                    } catch (ApiException e) {
                        e.printStackTrace();
                    }
                    accessTokenTimedCache = CacheUtil.newTimedCache((response.getExpiresIn() - 60) * 1000);
                    accessTokenTimedCache.put("accessToken", response.getAccessToken());
                }
            }
        }
        return accessTokenTimedCache;
    }
}

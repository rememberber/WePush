package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.fangxuele.tool.push.bean.account.DingAccountConfig;
import com.fangxuele.tool.push.bean.msg.DingMsg;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgDing;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.DingMsgMaker;
import com.fangxuele.tool.push.ui.form.msg.DingMsgForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public volatile static DefaultDingTalkClient robotClient;
    private TimedCache<String, String> accessTokenTimedCache;
    private DingMsgMaker dingMsgMaker;

    private static Map<Integer, TimedCache<String, String>> timedCacheMap = new HashMap<>();

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private DingAccountConfig dingAccountConfig;

    private TAccount tAccount;

    private TMsgDing tMsgDing;

    public DingMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        dingMsgMaker = new DingMsgMaker(tMsg);
        accessTokenTimedCache = getAccessTokenTimedCache(tMsg.getAccountId());
        this.dryRun = dryRun;

        tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        dingAccountConfig = JSON.parseObject(accountConfig, DingAccountConfig.class);

        tMsgDing = JSON.parseObject(tMsg.getContent(), TMsgDing.class);
    }

    @Override
    public SendResult send(String[] msgData) {
        if ("work".equals(tMsgDing.getRadioType())) {
            return sendWorkMsg(msgData);
        } else {
            return sendRobotMsg(msgData);
        }
    }

    public SendResult sendWorkMsg(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String userId = msgData[0];

            OapiMessageCorpconversationAsyncsendV2Request request2 = new OapiMessageCorpconversationAsyncsendV2Request();
            request2.setUseridList(userId);
            request2.setAgentId(Long.valueOf(dingAccountConfig.getAgentId()));
            request2.setToAllUser(false);

            DingMsg dingMsg = dingMsgMaker.makeMsg(msgData);
            OapiMessageCorpconversationAsyncsendV2Request.Msg msg = getMsg(dingMsg);
            request2.setMsg(msg);

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                OapiMessageCorpconversationAsyncsendV2Response response2 = defaultDingTalkClient.execute(request2, getAccessTokenTimedCache(tAccount.getId()).get("accessToken"));
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
            log.error(ExceptionUtils.getStackTrace(e));
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }

    public SendResult sendRobotMsg(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            DingTalkClient client = getRobotClient();
            OapiRobotSendRequest request2 = new OapiRobotSendRequest();
            DingMsg dingMsg = dingMsgMaker.makeMsg(msgData);
            if ("文本消息".equals(tMsgDing.getDingMsgType())) {
                request2.setMsgtype("text");
                OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
                text.setContent(dingMsg.getContent());
                request2.setText(text);
                OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
                if (msgData != null && StringUtils.isNotBlank(msgData[0])) {
                    List<String> mobiles = Lists.newArrayList();
                    mobiles.add(msgData[0]);
                    at.setAtMobiles(mobiles);
                } else {
                    at.setIsAtAll("true");
                }
                request2.setAt(at);
            } else if ("链接消息".equals(tMsgDing.getDingMsgType())) {
                request2.setMsgtype("link");
                OapiRobotSendRequest.Link link = new OapiRobotSendRequest.Link();
                link.setMessageUrl(dingMsg.getUrl());
                link.setPicUrl(dingMsg.getPicUrl());
                link.setTitle(dingMsg.getTitle());
                link.setText(dingMsg.getContent());
                request2.setLink(link);
            } else if ("markdown消息".equals(tMsgDing.getDingMsgType())) {
                request2.setMsgtype("markdown");
                OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
                markdown.setTitle(dingMsg.getTitle());
                markdown.setText(dingMsg.getContent());
                request2.setMarkdown(markdown);
            } else if ("卡片消息".equals(tMsgDing.getDingMsgType())) {
                request2.setMsgtype("actionCard");
                OapiRobotSendRequest.Actioncard actionCard = new OapiRobotSendRequest.Actioncard();
                actionCard.setTitle(dingMsg.getTitle());
                actionCard.setText(dingMsg.getContent());
                actionCard.setSingleTitle(dingMsg.getBtnTxt());
                actionCard.setSingleURL(dingMsg.getBtnUrl());
                request2.setActionCard(actionCard);
            }

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                OapiRobotSendResponse response2 = client.execute(request2);
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
        if ("文本消息".equals(tMsgDing.getDingMsgType())) {
            msg.setMsgtype("text");
            msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
            msg.getText().setContent(dingMsg.getContent());
        } else if ("链接消息".equals(tMsgDing.getDingMsgType())) {
            msg.setMsgtype("link");
            msg.setLink(new OapiMessageCorpconversationAsyncsendV2Request.Link());
            msg.getLink().setTitle(dingMsg.getTitle());
            msg.getLink().setText(dingMsg.getContent());
            msg.getLink().setMessageUrl(dingMsg.getUrl());
            msg.getLink().setPicUrl(dingMsg.getPicUrl());
        } else if ("markdown消息".equals(tMsgDing.getDingMsgType())) {
            msg.setMsgtype("markdown");
            msg.setMarkdown(new OapiMessageCorpconversationAsyncsendV2Request.Markdown());
            msg.getMarkdown().setText(dingMsg.getContent());
            msg.getMarkdown().setTitle(dingMsg.getTitle());
        } else if ("卡片消息".equals(tMsgDing.getDingMsgType())) {
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

    public DefaultDingTalkClient getRobotClient() {
        if (robotClient == null) {
            synchronized (PushControl.class) {
                if (robotClient == null) {
                    robotClient = new DefaultDingTalkClient(tMsgDing.getWebHook());
                }
            }
        }
        return robotClient;
    }

    public TimedCache<String, String> getAccessTokenTimedCache() {
        if (accessTokenTimedCache == null || StringUtils.isEmpty(accessTokenTimedCache.get("accessToken"))) {
            synchronized (PushControl.class) {
                if (accessTokenTimedCache == null || StringUtils.isEmpty(accessTokenTimedCache.get("accessToken"))) {
                    DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
                    OapiGettokenRequest request = new OapiGettokenRequest();
                    // TODO
                    String agentId = DingMsgForm.appNameToAgentIdMap.get(DingMsgForm.getInstance().getAppNameComboBox().getSelectedItem());
//                    TDingApp tDingApp = dingAppMapper.selectByAgentId(agentId);
//                    request.setAppkey(tDingApp.getAppKey());
//                    request.setAppsecret(tDingApp.getAppSecret());
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

    public static TimedCache<String, String> getAccessTokenTimedCache(Integer accountId) {
        if (timedCacheMap.containsKey(accountId)) {
            return timedCacheMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            DingAccountConfig dingAccountConfig = JSON.parseObject(accountConfig, DingAccountConfig.class);

            DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
            OapiGettokenRequest request = new OapiGettokenRequest();
            request.setAppkey(dingAccountConfig.getAppKey());
            request.setAppsecret(dingAccountConfig.getAppSecret());
            request.setHttpMethod("GET");
            OapiGettokenResponse response = null;
            try {
                response = client.execute(request);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            TimedCache<String, String> accessTokenTimedCache = CacheUtil.newTimedCache((response.getExpiresIn() - 60) * 1000);
            accessTokenTimedCache.put("accessToken", response.getAccessToken());

            timedCacheMap.put(accountId, accessTokenTimedCache);
            return accessTokenTimedCache;
        }

    }
}

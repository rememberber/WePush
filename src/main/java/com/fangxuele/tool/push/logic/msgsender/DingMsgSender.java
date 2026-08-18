package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.DingAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgDing;
import com.fangxuele.tool.push.logic.msgmaker.DingMsgMaker;
import com.fangxuele.tool.push.util.DingTalkApiUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 钉钉消息发送器
 * 接口文档：https://open.dingtalk.com/document/orgapp-server/asynchronous-sending-of-enterprise-session-messages
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/5.
 */
@Slf4j
public class DingMsgSender implements IMsgSender {
    /**
     * 获取access_token接口地址
     */
    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";

    /**
     * 工作通知消息接口地址
     */
    private static final String WORK_MSG_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

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

    public static void removeAccount(Integer account1Id) {
        timedCacheMap.remove(account1Id);
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

            TMsgDing dingMsg = dingMsgMaker.makeMsg(msgData);

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                JSONObject requestJson = new JSONObject();
                requestJson.put("userid_list", userId);
                requestJson.put("agent_id", Long.valueOf(dingAccountConfig.getAgentId()));
                requestJson.put("to_all_user", false);
                requestJson.put("msg", buildWorkMsg(dingMsg));

                String accessToken = getAccessTokenTimedCache(tAccount.getId()).get("accessToken");
                JSONObject response = DingTalkApiUtil.postJson(WORK_MSG_URL + "?access_token=" + accessToken, requestJson);
                if (response.getIntValue("errcode") != 0) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.getString("errmsg"));
                    log.error(response.getString("errmsg"));
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
            TMsgDing dingMsg = dingMsgMaker.makeMsg(msgData);
            JSONObject requestJson = new JSONObject();
            if ("文本消息".equals(tMsgDing.getDingMsgType())) {
                requestJson.put("msgtype", "text");
                JSONObject text = new JSONObject();
                text.put("content", dingMsg.getContent());
                requestJson.put("text", text);
                JSONObject at = new JSONObject();
                if (msgData != null && StringUtils.isNotBlank(msgData[0])) {
                    JSONArray atMobiles = new JSONArray();
                    atMobiles.add(msgData[0]);
                    at.put("atMobiles", atMobiles);
                } else {
                    at.put("isAtAll", true);
                }
                requestJson.put("at", at);
            } else if ("链接消息".equals(tMsgDing.getDingMsgType())) {
                requestJson.put("msgtype", "link");
                JSONObject link = new JSONObject();
                link.put("messageUrl", dingMsg.getUrl());
                link.put("picUrl", dingMsg.getPicUrl());
                link.put("title", dingMsg.getMsgTitle());
                link.put("text", dingMsg.getContent());
                requestJson.put("link", link);
            } else if ("markdown消息".equals(tMsgDing.getDingMsgType())) {
                requestJson.put("msgtype", "markdown");
                JSONObject markdown = new JSONObject();
                markdown.put("title", dingMsg.getMsgTitle());
                markdown.put("text", dingMsg.getContent());
                requestJson.put("markdown", markdown);
            } else if ("卡片消息".equals(tMsgDing.getDingMsgType())) {
                requestJson.put("msgtype", "actionCard");
                JSONObject actionCard = new JSONObject();
                actionCard.put("title", dingMsg.getMsgTitle());
                actionCard.put("text", dingMsg.getContent());
                actionCard.put("singleTitle", dingMsg.getBtnTxt());
                actionCard.put("singleURL", dingMsg.getBtnUrl());
                requestJson.put("actionCard", actionCard);
            }

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                JSONObject response = DingTalkApiUtil.postJson(tMsgDing.getWebHook(), requestJson);
                if (response.getIntValue("errcode") != 0) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.getString("errmsg"));
                    log.error(response.getString("errmsg"));
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

    /**
     * 组装工作通知消息体
     */
    private JSONObject buildWorkMsg(TMsgDing dingMsg) {
        JSONObject msg = new JSONObject();
        if ("文本消息".equals(tMsgDing.getDingMsgType())) {
            msg.put("msgtype", "text");
            JSONObject text = new JSONObject();
            text.put("content", dingMsg.getContent());
            msg.put("text", text);
        } else if ("链接消息".equals(tMsgDing.getDingMsgType())) {
            msg.put("msgtype", "link");
            JSONObject link = new JSONObject();
            link.put("title", dingMsg.getMsgTitle());
            link.put("text", dingMsg.getContent());
            link.put("messageUrl", dingMsg.getUrl());
            link.put("picUrl", dingMsg.getPicUrl());
            msg.put("link", link);
        } else if ("markdown消息".equals(tMsgDing.getDingMsgType())) {
            msg.put("msgtype", "markdown");
            JSONObject markdown = new JSONObject();
            markdown.put("text", dingMsg.getContent());
            markdown.put("title", dingMsg.getMsgTitle());
            msg.put("markdown", markdown);
        } else if ("卡片消息".equals(tMsgDing.getDingMsgType())) {
            msg.put("msgtype", "action_card");
            JSONObject actionCard = new JSONObject();
            actionCard.put("title", dingMsg.getMsgTitle());
            actionCard.put("markdown", dingMsg.getContent());
            actionCard.put("single_title", dingMsg.getBtnTxt());
            actionCard.put("single_url", dingMsg.getBtnUrl());
            msg.put("action_card", actionCard);
        }
        return msg;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    public static TimedCache<String, String> getAccessTokenTimedCache(Integer accountId) {
        if (timedCacheMap.containsKey(accountId)) {
            return timedCacheMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            DingAccountConfig dingAccountConfig = JSON.parseObject(accountConfig, DingAccountConfig.class);

            try {
                String url = GET_TOKEN_URL + "?appkey=" + URLEncoder.encode(dingAccountConfig.getAppKey(), "UTF-8")
                        + "&appsecret=" + URLEncoder.encode(dingAccountConfig.getAppSecret(), "UTF-8");
                JSONObject response = DingTalkApiUtil.get(url);
                if (response.getIntValue("errcode") != 0) {
                    log.error("获取钉钉accessToken失败：{}", response.getString("errmsg"));
                    throw new RuntimeException("获取钉钉accessToken失败：" + response.getString("errmsg"));
                }
                TimedCache<String, String> accessTokenTimedCache = CacheUtil.newTimedCache((response.getLongValue("expires_in") - 60) * 1000);
                accessTokenTimedCache.put("accessToken", response.getString("access_token"));

                timedCacheMap.put(accountId, accessTokenTimedCache);
                return accessTokenTimedCache;
            } catch (Exception e) {
                log.error(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }
        }

    }
}

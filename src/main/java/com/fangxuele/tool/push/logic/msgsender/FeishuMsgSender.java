package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.bean.account.FeishuAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgFeishu;
import com.fangxuele.tool.push.logic.msgmaker.FeishuMsgMaker;
import com.fangxuele.tool.push.util.FeishuBotSupport;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 飞书群自定义机器人消息发送器。
 *
 * @see <a href="https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot">飞书自定义机器人文档</a>
 */
@Slf4j
public class FeishuMsgSender implements IMsgSender {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final Map<String, FeishuRateLimiter> RATE_LIMITERS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ACCOUNT_WEBHOOKS = new ConcurrentHashMap<>();
    private static final TAccountMapper ACCOUNT_MAPPER = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static final TMsgMapper MSG_MAPPER = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private final FeishuMsgMaker messageMaker;
    private final FeishuAccountConfig accountConfig;
    private final Integer dryRun;
    private final OkHttpClient httpClient;

    public FeishuMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = MSG_MAPPER.selectByPrimaryKey(msgId);
        if (tMsg == null) {
            throw new IllegalArgumentException("飞书消息不存在：" + msgId);
        }
        TAccount account = ACCOUNT_MAPPER.selectByPrimaryKey(tMsg.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("飞书机器人账号不存在：" + tMsg.getAccountId());
        }
        this.messageMaker = new FeishuMsgMaker(tMsg);
        this.accountConfig = JSON.parseObject(account.getAccountConfig(), FeishuAccountConfig.class);
        if (this.accountConfig == null) {
            throw new IllegalArgumentException("飞书机器人账号配置为空");
        }
        FeishuBotSupport.validateWebhook(this.accountConfig.getWebhook());
        ACCOUNT_WEBHOOKS.put(account.getId(), this.accountConfig.getWebhook().trim());
        this.httpClient = HttpClientRegistry.get(MessageTypeEnum.FEISHU_CODE, account.getId());
        this.dryRun = dryRun;
    }

    public static void removeWebhook(String webhook) {
        if (StringUtils.isNotBlank(webhook)) {
            RATE_LIMITERS.remove(webhook.trim());
        }
    }

    public static void removeAccount(Integer accountId) {
        removeWebhook(ACCOUNT_WEBHOOKS.remove(accountId));
        HttpClientRegistry.invalidate(MessageTypeEnum.FEISHU_CODE, accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.FEISHU_CODE, accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult result = new SendResult();
        try {
            TMsgFeishu message = messageMaker.makeMsg(msgData);
            String mentionOpenId = resolveMention(message.getMentionType(), msgData);
            JSONObject payload = FeishuBotSupport.buildPayload(
                    message.getFeishuMsgType(),
                    message.getTitle(),
                    message.getContent(),
                    accountConfig.getKeyword(),
                    mentionOpenId);
            FeishuBotSupport.addSignature(payload, Instant.now().getEpochSecond(), accountConfig.getSecret());
            String requestJson = payload.toJSONString();
            FeishuBotSupport.validatePayloadSize(requestJson);

            if (Integer.valueOf(1).equals(dryRun)) {
                result.setSuccess(true);
                result.setInfo("飞书消息校验通过");
                return result;
            }

            limiter().acquire();
            Request request = new Request.Builder()
                    .url(accountConfig.getWebhook().trim())
                    .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                    .build();
            OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
            result.setHttpStatus(response.statusCode());
            result.setRetryAfterMillis(response.retryAfterMillis());
            if (!response.isSuccessful()) {
                throw new IllegalStateException("飞书 HTTP 请求失败（" + response.statusCode() + "）：" + response.body());
            }
            FeishuBotSupport.validateResponse(response.body());
            result.setSuccess(true);
            result.setInfo(response.body());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.setSuccess(false);
            result.setInfo("等待飞书发送限流时任务被中断");
            return result;
        } catch (Exception e) {
            result.setSuccess(false);
            result.setInfo(e.getMessage() == null ? e.toString() : e.getMessage());
            log.error("发送飞书机器人消息失败：{}", ExceptionUtils.getStackTrace(e));
            return result;
        }
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return send(msgData);
    }

    private FeishuRateLimiter limiter() {
        String webhook = accountConfig.getWebhook().trim();
        return RATE_LIMITERS.computeIfAbsent(webhook, key -> new FeishuRateLimiter());
    }

    private static String firstValue(String[] msgData) {
        if (msgData == null || msgData.length == 0) {
            return "";
        }
        return StringUtils.defaultString(msgData[0]);
    }

    private static String resolveMention(String mentionType, String[] msgData) {
        if (FeishuBotSupport.MENTION_ALL.equals(mentionType)) {
            return "all";
        }
        if (FeishuBotSupport.MENTION_FIRST_COLUMN.equals(mentionType)) {
            String openId = firstValue(msgData);
            if (StringUtils.isBlank(openId)) {
                throw new IllegalArgumentException("消息设置为 @ 数据第1列，但第1列 open_id 为空");
            }
            return openId;
        }
        return "";
    }

    /**
     * 同时落实飞书的 5 次/秒和 100 次/分钟限制。实例按 Webhook 共享。
     */
    private static final class FeishuRateLimiter {
        private static final int SECOND_LIMIT = 5;
        private static final int MINUTE_LIMIT = 100;
        private static final long SECOND_WINDOW_MILLIS = 1_000L;
        private static final long MINUTE_WINDOW_MILLIS = 60_000L;

        private final Deque<Long> secondWindow = new ArrayDeque<>();
        private final Deque<Long> minuteWindow = new ArrayDeque<>();

        private synchronized void acquire() throws InterruptedException {
            while (true) {
                long now = System.currentTimeMillis();
                prune(secondWindow, now - SECOND_WINDOW_MILLIS);
                prune(minuteWindow, now - MINUTE_WINDOW_MILLIS);
                if (secondWindow.size() < SECOND_LIMIT && minuteWindow.size() < MINUTE_LIMIT) {
                    secondWindow.addLast(now);
                    minuteWindow.addLast(now);
                    return;
                }

                long secondWait = secondWindow.size() >= SECOND_LIMIT
                        ? SECOND_WINDOW_MILLIS - (now - secondWindow.peekFirst()) : 0L;
                long minuteWait = minuteWindow.size() >= MINUTE_LIMIT
                        ? MINUTE_WINDOW_MILLIS - (now - minuteWindow.peekFirst()) : 0L;
                wait(Math.max(1L, Math.max(secondWait, minuteWait)));
            }
        }

        private void prune(Deque<Long> timestamps, long threshold) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= threshold) {
                timestamps.removeFirst();
            }
        }
    }
}

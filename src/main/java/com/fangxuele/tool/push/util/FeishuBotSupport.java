package com.fangxuele.tool.push.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * 飞书自定义机器人协议辅助方法。
 */
public final class FeishuBotSupport {
    public static final String TYPE_TEXT = "文本消息";
    public static final String TYPE_POST = "富文本消息";
    public static final String TYPE_CARD = "消息卡片";
    public static final String TYPE_RAW_JSON = "原始JSON";

    public static final String MENTION_NONE = "不@任何人";
    public static final String MENTION_FIRST_COLUMN = "@数据第1列open_id";
    public static final String MENTION_ALL = "@所有人";

    /** 飞书自定义机器人请求体上限。 */
    public static final int MAX_PAYLOAD_BYTES = 20 * 1024;

    private FeishuBotSupport() {
    }

    /**
     * 校验国内飞书和 Lark 官方自定义机器人 Webhook。
     */
    public static void validateWebhook(String webhook) {
        if (StringUtils.isBlank(webhook)) {
            throw new IllegalArgumentException("Webhook 不能为空");
        }
        try {
            URI uri = URI.create(webhook.trim());
            String host = uri.getHost();
            String path = uri.getPath();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !("open.feishu.cn".equalsIgnoreCase(host)
                    || "open.larksuite.com".equalsIgnoreCase(host))
                    || path == null
                    || !path.startsWith("/open-apis/bot/v2/hook/")) {
                throw new IllegalArgumentException("Webhook 必须是飞书或 Lark 官方自定义机器人地址");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Webhook")) {
                throw e;
            }
            throw new IllegalArgumentException("Webhook 格式不正确", e);
        }
    }

    /**
     * 按飞书签名校验规则生成签名。
     */
    public static String sign(long timestamp, String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("签名密钥不能为空");
        }
        String stringToSign = timestamp + "\n" + secret;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成飞书机器人签名失败", e);
        }
    }

    /**
     * 构造飞书请求体。卡片模式的 content 是 card 对象，原始 JSON 模式则是完整请求体。
     */
    public static JSONObject buildPayload(String messageType, String title, String content,
                                          String keyword, String mentionOpenId) {
        String normalizedContent = StringUtils.defaultString(content);
        switch (messageType) {
            case TYPE_TEXT:
                return buildTextPayload(withKeyword(normalizedContent, keyword), mentionOpenId);
            case TYPE_POST:
                return buildPostPayload(StringUtils.defaultString(title),
                        withKeyword(normalizedContent, keyword), mentionOpenId);
            case TYPE_CARD:
                JSONObject card = parseObject(normalizedContent, "消息卡片 JSON");
                JSONObject cardPayload = new JSONObject(true);
                cardPayload.put("msg_type", "interactive");
                cardPayload.put("card", card);
                return cardPayload;
            case TYPE_RAW_JSON:
                return parseObject(normalizedContent, "原始请求 JSON");
            default:
                throw new IllegalArgumentException("不支持的飞书消息类型：" + messageType);
        }
    }

    public static void addSignature(JSONObject payload, long timestamp, String secret) {
        if (StringUtils.isNotBlank(secret)) {
            payload.put("timestamp", String.valueOf(timestamp));
            payload.put("sign", sign(timestamp, secret));
        }
    }

    public static void validatePayloadSize(String payloadJson) {
        int bytes = payloadJson.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("请求体为 " + bytes + " 字节，超过飞书机器人 20 KB 上限");
        }
    }

    /**
     * 校验 HTTP 200 后的飞书业务响应。兼容新版 code 和旧版 StatusCode 字段。
     */
    public static void validateResponse(String responseBody) {
        JSONObject response = parseObject(responseBody, "飞书响应");
        Integer code = null;
        String message = null;
        if (response.containsKey("code")) {
            code = response.getInteger("code");
            message = response.getString("msg");
        } else if (response.containsKey("StatusCode")) {
            code = response.getInteger("StatusCode");
            message = response.getString("StatusMessage");
        }
        if (code == null) {
            throw new IllegalStateException("飞书响应缺少业务状态码");
        }
        if (code != 0) {
            throw new IllegalStateException("飞书返回错误（" + code + "）："
                    + StringUtils.defaultIfBlank(message, responseBody));
        }
    }

    private static JSONObject buildTextPayload(String content, String mentionOpenId) {
        JSONObject payload = new JSONObject(true);
        payload.put("msg_type", "text");
        JSONObject text = new JSONObject(true);
        text.put("text", mentionText(mentionOpenId) + content);
        payload.put("content", text);
        return payload;
    }

    private static JSONObject buildPostPayload(String title, String content, String mentionOpenId) {
        JSONArray paragraph = new JSONArray();
        if (StringUtils.isNotBlank(mentionOpenId)) {
            JSONObject at = new JSONObject(true);
            at.put("tag", "at");
            at.put("user_id", normalizeMention(mentionOpenId));
            paragraph.add(at);
        }
        JSONObject text = new JSONObject(true);
        text.put("tag", "text");
        text.put("text", content);
        paragraph.add(text);

        JSONArray paragraphs = new JSONArray();
        paragraphs.add(paragraph);
        JSONObject locale = new JSONObject(true);
        locale.put("title", title);
        locale.put("content", paragraphs);
        JSONObject post = new JSONObject(true);
        post.put("zh_cn", locale);
        JSONObject contentObject = new JSONObject(true);
        contentObject.put("post", post);
        JSONObject payload = new JSONObject(true);
        payload.put("msg_type", "post");
        payload.put("content", contentObject);
        return payload;
    }

    private static String withKeyword(String content, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return content;
        }
        return keyword.trim() + " " + content;
    }

    private static String mentionText(String mentionOpenId) {
        if (StringUtils.isBlank(mentionOpenId)) {
            return "";
        }
        String id = normalizeMention(mentionOpenId);
        String display = "all".equals(id) ? "所有人" : "用户";
        return "<at user_id=\"" + id + "\">" + display + "</at> ";
    }

    private static String normalizeMention(String mentionOpenId) {
        String id = mentionOpenId.trim();
        if ("@all".equalsIgnoreCase(id) || "all".equalsIgnoreCase(id)) {
            return "all";
        }
        if (!id.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("飞书 open_id 格式不正确");
        }
        return id;
    }

    private static JSONObject parseObject(String json, String name) {
        if (StringUtils.isBlank(json)) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        try {
            Object value = JSON.parse(json);
            if (!(value instanceof JSONObject)) {
                throw new IllegalArgumentException(name + "必须是 JSON 对象");
            }
            return (JSONObject) value;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(name + "格式不正确：" + e.getMessage(), e);
        }
    }
}

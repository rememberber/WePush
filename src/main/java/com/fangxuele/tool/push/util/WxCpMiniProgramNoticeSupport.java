package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.domain.WxCpMiniProgramContentItem;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 企业微信小程序通知消息的协议校验与有序内容项转换。
 */
public final class WxCpMiniProgramNoticeSupport {
    public static final String MESSAGE_TYPE = "小程序通知消息";
    public static final int MAX_CONTENT_ITEMS = 10;
    public static final int DEFAULT_DUPLICATE_CHECK_INTERVAL = 1800;
    public static final int MAX_DUPLICATE_CHECK_INTERVAL = 4 * 60 * 60;

    private WxCpMiniProgramNoticeSupport() {
    }

    /**
     * 校验已经完成变量渲染的消息，并转换为 SDK 需要的有序 Map。
     */
    public static LinkedHashMap<String, String> validateAndBuildContentItems(
            String appId,
            String page,
            String title,
            String description,
            List<WxCpMiniProgramContentItem> contentItems,
            Boolean enableDuplicateCheck,
            Integer duplicateCheckInterval) {
        if (StringUtils.isBlank(appId)) {
            throw new IllegalArgumentException("小程序 AppId 不能为空");
        }
        if (!appId.matches("wx[A-Za-z0-9]{16}")) {
            throw new IllegalArgumentException("小程序 AppId 格式不正确");
        }
        validateTextLength(title, "标题", 4, 12, false);
        validateTextLength(description, "描述", 4, 12, true);
        if (utf8Length(page) > 1024) {
            throw new IllegalArgumentException("小程序页面路径不能超过 1024 字节");
        }

        List<WxCpMiniProgramContentItem> normalizedItems = contentItems == null ? List.of() : contentItems;
        if (normalizedItems.size() > MAX_CONTENT_ITEMS) {
            throw new IllegalArgumentException("小程序通知内容项最多允许 10 个");
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < normalizedItems.size(); i++) {
            WxCpMiniProgramContentItem item = normalizedItems.get(i);
            String key = item == null ? "" : StringUtils.defaultString(item.getKey()).trim();
            String value = item == null ? "" : StringUtils.defaultString(item.getValue()).trim();
            if (StringUtils.isBlank(key) && StringUtils.isBlank(value)) {
                continue;
            }
            validateTextLength(key, "第 " + (i + 1) + " 个内容项的键", 0, 10, true);
            validateTextLength(value, "第 " + (i + 1) + " 个内容项的值", 0, 30, true);
            if (result.containsKey(key)) {
                throw new IllegalArgumentException("小程序通知内容项的键不能重复：" + key);
            }
            result.put(key, value);
        }

        if (Boolean.TRUE.equals(enableDuplicateCheck)) {
            int interval = duplicateCheckInterval == null
                    ? DEFAULT_DUPLICATE_CHECK_INTERVAL : duplicateCheckInterval;
            if (interval <= 0 || interval > MAX_DUPLICATE_CHECK_INTERVAL) {
                throw new IllegalArgumentException("重复消息检查间隔必须在 1 到 14400 秒之间");
            }
        }
        return result;
    }

    public static void validateRecipient(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("企业微信成员 UserId 不能为空");
        }
        if ("@all".equalsIgnoreCase(userId.trim())) {
            throw new IllegalArgumentException("企业微信小程序通知消息不支持 @all 全员发送");
        }
    }

    private static void validateTextLength(String value, String name, int min, int max, boolean optional) {
        String text = StringUtils.defaultString(value).trim();
        if (optional && text.isEmpty()) {
            return;
        }
        int length = text.codePointCount(0, text.length());
        if (length < min || length > max) {
            throw new IllegalArgumentException(name + "长度必须为 " + min + " 到 " + max + " 个字符");
        }
    }

    private static int utf8Length(String value) {
        return StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8).length;
    }
}

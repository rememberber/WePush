package com.fangxuele.wepush.next.provider.standard;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

enum BotVendor {
    FEISHU("wepush.bot.feishu", "Feishu / Lark Bot", "feishu-bot", 5,
            Set.of("TEXT", "POST", "INTERACTIVE", "RAW"), 20 * 1024,
            5, Duration.ofSeconds(1), 100, Duration.ofMinutes(1)),
    DINGTALK("wepush.bot.dingtalk", "DingTalk Bot", "dingtalk-bot", 2,
            Set.of("TEXT", "MARKDOWN", "LINK", "ACTION_CARD", "RAW"), 20 * 1024,
            20, Duration.ofMinutes(1), 0, Duration.ZERO),
    WECOM("wepush.bot.wecom", "WeCom Group Bot", "wecom-bot", 2,
            Set.of("TEXT", "MARKDOWN", "RAW"), 20 * 1024,
            20, Duration.ofMinutes(1), 0, Duration.ZERO);

    final String providerId;
    final String displayName;
    final String slug;
    final int maximumConcurrency;
    final Set<String> messageTypes;
    final int maximumPayloadBytes;
    final int firstLimit;
    final Duration firstWindow;
    final int secondLimit;
    final Duration secondWindow;

    BotVendor(String providerId, String displayName, String slug, int maximumConcurrency,
              Set<String> messageTypes, int maximumPayloadBytes,
              int firstLimit, Duration firstWindow, int secondLimit, Duration secondWindow) {
        this.providerId = providerId;
        this.displayName = displayName;
        this.slug = slug;
        this.maximumConcurrency = maximumConcurrency;
        this.messageTypes = messageTypes;
        this.maximumPayloadBytes = maximumPayloadBytes;
        this.firstLimit = firstLimit;
        this.firstWindow = firstWindow;
        this.secondLimit = secondLimit;
        this.secondWindow = secondWindow;
    }

    void validateEndpoint(URI uri, boolean allowTestEndpoint) {
        if (allowTestEndpoint && ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null) return;
        boolean defaultHttpsPort = uri.getPort() == -1 || uri.getPort() == 443;
        boolean valid = defaultHttpsPort && switch (this) {
            case FEISHU -> "https".equalsIgnoreCase(uri.getScheme())
                    && Set.of("open.feishu.cn", "open.larksuite.com").contains(lower(uri.getHost()))
                    && uri.getPath() != null && uri.getPath().startsWith("/open-apis/bot/v2/hook/");
            case DINGTALK -> "https".equalsIgnoreCase(uri.getScheme())
                    && "oapi.dingtalk.com".equalsIgnoreCase(uri.getHost())
                    && "/robot/send".equals(uri.getPath()) && queryContains(uri, "access_token");
            case WECOM -> "https".equalsIgnoreCase(uri.getScheme())
                    && "qyapi.weixin.qq.com".equalsIgnoreCase(uri.getHost())
                    && "/cgi-bin/webhook/send".equals(uri.getPath()) && queryContains(uri, "key");
        };
        if (!valid || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw StandardProviderSupport.invalid("webhook", "WEBHOOK_NOT_OFFICIAL",
                    displayName + " webhook must use the official HTTPS endpoint");
        }
    }

    private static boolean queryContains(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null) return false;
        return java.util.Arrays.stream(query.split("&"))
                .anyMatch(value -> value.startsWith(name + "=") && value.length() > name.length() + 1);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }
}

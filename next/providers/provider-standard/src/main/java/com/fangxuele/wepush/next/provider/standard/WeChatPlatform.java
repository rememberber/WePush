package com.fangxuele.wepush.next.provider.standard;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

enum WeChatPlatform {
    OFFICIAL("wepush.wechat.official", "WeChat Official Account", "wechat-official",
            Set.of("TEMPLATE", "SUBSCRIBE", "CUSTOM"), "api.weixin.qq.com", 10),
    MINI("wepush.wechat.mini", "WeChat Mini Program", "wechat-mini",
            Set.of("SUBSCRIBE", "UNIFORM"), "api.weixin.qq.com", 10),
    WECOM_APP("wepush.wecom.app", "WeCom Application", "wecom-app",
            Set.of("APP"), "qyapi.weixin.qq.com", 10);

    final String providerId;
    final String displayName;
    final String slug;
    final Set<String> messageTypes;
    final String officialHost;
    final int maximumConcurrency;

    WeChatPlatform(String providerId, String displayName, String slug,
                   Set<String> messageTypes, String officialHost, int maximumConcurrency) {
        this.providerId = providerId;
        this.displayName = displayName;
        this.slug = slug;
        this.messageTypes = messageTypes;
        this.officialHost = officialHost;
        this.maximumConcurrency = maximumConcurrency;
    }

    URI productionBase() { return URI.create("https://" + officialHost); }

    String tokenPath() {
        return this == WECOM_APP ? "/cgi-bin/gettoken" : "/cgi-bin/token";
    }

    String sendPath(String messageType) {
        if (this == OFFICIAL) return switch (messageType) {
            case "TEMPLATE" -> "/cgi-bin/message/template/send";
            case "SUBSCRIBE" -> "/cgi-bin/message/subscribe/bizsend";
            case "CUSTOM" -> "/cgi-bin/message/custom/send";
            default -> throw new IllegalArgumentException("Unsupported official account message type");
        };
        if (this == MINI) return switch (messageType) {
            case "SUBSCRIBE" -> "/cgi-bin/message/subscribe/send";
            case "UNIFORM" -> "/cgi-bin/message/wxopen/template/uniform_send";
            default -> throw new IllegalArgumentException("Unsupported Mini Program message type");
        };
        return "/cgi-bin/message/send";
    }

    void validateBase(URI base) {
        if (base == null || base.getHost() == null) throw new IllegalArgumentException("API base is required");
        boolean official = base.getScheme().equals("https")
                && base.getHost().equalsIgnoreCase(officialHost)
                && (base.getPort() == -1 || base.getPort() == 443)
                && (base.getPath().isEmpty() || base.getPath().equals("/"));
        boolean test = base.getScheme().equals("http")
                && (base.getHost().equals("127.0.0.1") || base.getHost().equals("localhost")
                || base.getHost().equals("::1"));
        if (!official && !test) throw new IllegalArgumentException(
                displayName + " API base must use the official endpoint");
    }
}

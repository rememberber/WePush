package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.SecretRef;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

final class WeChatProviderConfig {
    private WeChatProviderConfig() { }

    static Account account(WeChatPlatform platform, ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, platform.displayName + " account");
        String principalId = StandardProviderSupport.requiredText(root,
                platform == WeChatPlatform.WECOM_APP ? "corpId" : "appId");
        SecretRef secret = StandardProviderSupport.requiredSecret(root,
                platform == WeChatPlatform.WECOM_APP ? "corpSecret" : "appSecret");
        int agentId = platform == WeChatPlatform.WECOM_APP
                ? StandardProviderSupport.integer(root, "agentId", -1, 0, Integer.MAX_VALUE) : 0;
        Duration connectTimeout = StandardProviderSupport.duration(root, "connectTimeout",
                Duration.ofSeconds(10), Duration.ofMinutes(2));
        return new Account(principalId, secret, agentId, connectTimeout);
    }

    static Message message(WeChatPlatform platform, ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, platform.displayName + " message");
        String type = StandardProviderSupport.optionalText(root, "type",
                platform == WeChatPlatform.WECOM_APP ? "APP" : "").trim().toUpperCase(Locale.ROOT);
        if (!platform.messageTypes.contains(type)) {
            throw StandardProviderSupport.invalid("type", "MESSAGE_TYPE_UNSUPPORTED",
                    "Message type is not supported by " + platform.displayName);
        }
        String payload = StandardProviderSupport.requiredText(root, "payloadJsonTemplate");
        String rendered = StandardProviderSupport.renderJson(payload,
                new RecipientRecord("validation", 0, Map.of()));
        try {
            if (!StandardProviderSupport.JSON.readTree(rendered).isObject()) {
                throw StandardProviderSupport.invalid("payloadJsonTemplate", "PAYLOAD_OBJECT_REQUIRED",
                        "payloadJsonTemplate must render to a JSON object");
            }
        } catch (ProviderConfigException problem) {
            throw problem;
        } catch (Exception problem) {
            throw StandardProviderSupport.invalid("payloadJsonTemplate", "PAYLOAD_JSON_INVALID",
                    "payloadJsonTemplate must be valid JSON");
        }
        boolean duplicateCheck = platform == WeChatPlatform.WECOM_APP
                && StandardProviderSupport.optionalBoolean(root, "enableDuplicateCheck", true);
        int duplicateInterval = platform == WeChatPlatform.WECOM_APP
                ? StandardProviderSupport.integer(root, "duplicateCheckInterval", 1800, 1, 14_400) : 0;
        return new Message(type, payload, duplicateCheck, duplicateInterval);
    }

    record Account(String principalId, SecretRef credential, int agentId,
                   Duration connectTimeout) { }

    record Message(String type, String payloadJsonTemplate, boolean enableDuplicateCheck,
                   int duplicateCheckInterval) { }
}

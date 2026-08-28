package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretRef;

final class BotProviderConfig {
    private BotProviderConfig() { }

    static Account account(BotVendor vendor, ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, vendor.displayName + " account");
        SecretRef webhook = StandardProviderSupport.requiredSecret(root, "webhook");
        SecretRef signingSecret = StandardProviderSupport.optionalSecret(root, "signingSecret");
        if (vendor == BotVendor.WECOM && signingSecret != null) {
            throw StandardProviderSupport.invalid("signingSecret", "SIGNING_NOT_SUPPORTED",
                    "WeCom group bot does not use a separate signing secret");
        }
        String keyword = StandardProviderSupport.optionalText(root, "keyword", "").trim();
        return new Account(webhook, signingSecret, keyword);
    }

    static Message message(BotVendor vendor, ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, vendor.displayName + " message");
        String type = StandardProviderSupport.requiredText(root, "type").toUpperCase(java.util.Locale.ROOT);
        if (!vendor.messageTypes.contains(type)) {
            throw StandardProviderSupport.invalid("type", "MESSAGE_TYPE_UNSUPPORTED",
                    "Message type is not supported by " + vendor.displayName);
        }
        String title = StandardProviderSupport.optionalText(root, "titleTemplate", "");
        String content = StandardProviderSupport.optionalText(root, "contentTemplate", "");
        String raw = StandardProviderSupport.optionalText(root, "rawJsonTemplate", "");
        String url = StandardProviderSupport.optionalText(root, "urlTemplate", "");
        String pictureUrl = StandardProviderSupport.optionalText(root, "pictureUrlTemplate", "");
        String buttonText = StandardProviderSupport.optionalText(root, "buttonTextTemplate", "");
        String buttonUrl = StandardProviderSupport.optionalText(root, "buttonUrlTemplate", "");
        boolean mentionAll = StandardProviderSupport.optionalBoolean(root, "mentionAll", false);
        String mentionField = StandardProviderSupport.optionalText(root, "mentionField", "").trim();

        if (type.equals("RAW") || type.equals("INTERACTIVE")) {
            if (raw.isBlank()) throw StandardProviderSupport.invalid("rawJsonTemplate", "RAW_JSON_REQUIRED",
                    "rawJsonTemplate is required for " + type);
            StandardProviderSupport.renderJson(raw,
                    new com.fangxuele.wepush.next.core.api.RecipientRecord("validation", 0, java.util.Map.of()));
        } else if (content.isBlank()) {
            throw StandardProviderSupport.invalid("contentTemplate", "MESSAGE_CONTENT_REQUIRED",
                    "contentTemplate is required for " + type);
        }
        if (type.equals("POST") || type.equals("MARKDOWN") || type.equals("LINK")
                || type.equals("ACTION_CARD")) {
            if (title.isBlank()) throw StandardProviderSupport.invalid("titleTemplate", "MESSAGE_TITLE_REQUIRED",
                    "titleTemplate is required for " + type);
        }
        if (type.equals("LINK") && url.isBlank()) {
            throw StandardProviderSupport.invalid("urlTemplate", "MESSAGE_URL_REQUIRED",
                    "urlTemplate is required for LINK");
        }
        if (type.equals("ACTION_CARD") && (buttonText.isBlank() || buttonUrl.isBlank())) {
            throw StandardProviderSupport.invalid("buttonTextTemplate", "BUTTON_REQUIRED",
                    "buttonTextTemplate and buttonUrlTemplate are required for ACTION_CARD");
        }
        return new Message(type, title, content, raw, url, pictureUrl, buttonText, buttonUrl,
                mentionAll, mentionField);
    }

    record Account(SecretRef webhook, SecretRef signingSecret, String keyword) { }

    record Message(String type, String titleTemplate, String contentTemplate, String rawJsonTemplate,
                   String urlTemplate, String pictureUrlTemplate, String buttonTextTemplate,
                   String buttonUrlTemplate, boolean mentionAll, String mentionField) { }
}

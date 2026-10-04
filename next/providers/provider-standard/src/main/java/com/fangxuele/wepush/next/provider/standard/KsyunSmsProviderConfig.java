package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretRef;

import java.time.Duration;

final class KsyunSmsProviderConfig {
    private KsyunSmsProviderConfig() { }

    static Account account(ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, "Ksyun SMS account");
        String accessKeyId = StandardProviderSupport.requiredText(root, "accessKeyId");
        SecretRef accessKeySecret = StandardProviderSupport.requiredSecret(root, "accessKeySecret");
        String region = StandardProviderSupport.optionalText(root, "region", "cn-beijing-6").trim();
        if (region.isBlank() || !region.equals("cn-beijing-6") && !region.equals("cn-guangzhou-1")) {
            throw StandardProviderSupport.invalid("region", "INVALID_REGION",
                    "region contains unsupported characters");
        }
        Duration connectTimeout = StandardProviderSupport.duration(root, "connectTimeout",
                Duration.ofSeconds(10), Duration.ofMinutes(2));
        return new Account(accessKeyId, accessKeySecret, region, connectTimeout);
    }

    static Message message(ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, "Ksyun SMS message");
        String signName = StandardProviderSupport.requiredText(root, "signName");
        String templateId = StandardProviderSupport.requiredText(root, "templateId");
        String templateParams = StandardProviderSupport.optionalText(root,
                "templateParamJsonTemplate", "{}");
        String extId = StandardProviderSupport.optionalText(root, "extId", "").trim();
        if (signName.length() > 100) throw StandardProviderSupport.invalid("signName", "SIGN_NAME_TOO_LONG",
                "signName must not exceed 100 characters");
        if (!templateId.matches("[A-Za-z0-9_-]{1,100}")) {
            throw StandardProviderSupport.invalid("templateId", "INVALID_TEMPLATE_CODE",
                    "templateId contains unsupported characters");
        }
        JsonNode parsed;
        try {
            parsed = StandardProviderSupport.JSON.readTree(templateParams.replaceAll("\\{\\{[^}]+}}", "value"));
        } catch (Exception problem) {
            throw StandardProviderSupport.invalid("templateParamJsonTemplate", "INVALID_TEMPLATE_PARAMS_JSON",
                    "templateParamJsonTemplate must be a JSON object template");
        }
        if (parsed == null || !parsed.isObject()) {
            throw StandardProviderSupport.invalid("templateParamJsonTemplate", "TEMPLATE_PARAMS_OBJECT_REQUIRED",
                    "templateParamJsonTemplate must render to a JSON object");
        }
        if (!extId.isEmpty() && !extId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw StandardProviderSupport.invalid("extId", "INVALID_UP_EXTEND_CODE",
                    "extId must contain 1 to 64 letters, digits, underscores or hyphens");
        }
        return new Message(signName, templateId, templateParams, extId);
    }

    record Account(String accessKeyId, SecretRef accessKeySecret, String region,
                   Duration connectTimeout) { }

    record Message(String signName, String templateId, String templateParamJsonTemplate,
                   String extId) { }
}

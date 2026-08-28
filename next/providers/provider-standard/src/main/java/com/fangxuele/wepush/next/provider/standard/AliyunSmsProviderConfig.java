package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretRef;

import java.time.Duration;

final class AliyunSmsProviderConfig {
    private AliyunSmsProviderConfig() { }

    static Account account(ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, "Aliyun SMS account");
        String accessKeyId = StandardProviderSupport.requiredText(root, "accessKeyId");
        SecretRef accessKeySecret = StandardProviderSupport.requiredSecret(root, "accessKeySecret");
        String regionId = StandardProviderSupport.optionalText(root, "regionId", "cn-hangzhou").trim();
        if (regionId.isBlank() || !regionId.matches("[A-Za-z0-9-]{2,40}")) {
            throw StandardProviderSupport.invalid("regionId", "INVALID_REGION",
                    "regionId contains unsupported characters");
        }
        Duration connectTimeout = StandardProviderSupport.duration(root, "connectTimeout",
                Duration.ofSeconds(10), Duration.ofMinutes(2));
        return new Account(accessKeyId, accessKeySecret, regionId, connectTimeout);
    }

    static Message message(ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, "Aliyun SMS message");
        String signName = StandardProviderSupport.requiredText(root, "signName");
        String templateCode = StandardProviderSupport.requiredText(root, "templateCode");
        String templateParams = StandardProviderSupport.optionalText(root,
                "templateParamJsonTemplate", "{}");
        String smsUpExtendCode = StandardProviderSupport.optionalText(root, "smsUpExtendCode", "").trim();
        if (signName.length() > 100) throw StandardProviderSupport.invalid("signName", "SIGN_NAME_TOO_LONG",
                "signName must not exceed 100 characters");
        if (!templateCode.matches("[A-Za-z0-9_-]{1,100}")) {
            throw StandardProviderSupport.invalid("templateCode", "INVALID_TEMPLATE_CODE",
                    "templateCode contains unsupported characters");
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
        if (!smsUpExtendCode.isEmpty() && !smsUpExtendCode.matches("[A-Za-z0-9]{1,20}")) {
            throw StandardProviderSupport.invalid("smsUpExtendCode", "INVALID_UP_EXTEND_CODE",
                    "smsUpExtendCode must contain 1 to 20 letters or digits");
        }
        return new Message(signName, templateCode, templateParams, smsUpExtendCode);
    }

    record Account(String accessKeyId, SecretRef accessKeySecret, String regionId,
                   Duration connectTimeout) { }

    record Message(String signName, String templateCode, String templateParamJsonTemplate,
                   String smsUpExtendCode) { }
}

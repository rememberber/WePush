package com.fangxuele.wepush.next.plugin.carriersms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.SecretRef;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CarrierProviderSupport {
    static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");

    private CarrierProviderSupport() { }

    static JsonNode object(ConfigDocument document, String kind) {
        if (!ConfigDocument.JSON_MEDIA_TYPE.equals(document.mediaType())) {
            throw invalid("", "UNSUPPORTED_MEDIA_TYPE", kind + " must use application/json");
        }
        try {
            JsonNode root = JSON.readTree(document.canonicalContent());
            if (root == null || !root.isObject()) throw invalid("", "INVALID_JSON_OBJECT", kind + " must be an object");
            return root;
        } catch (JsonProcessingException problem) {
            throw invalid("", "INVALID_JSON", kind + " is not valid JSON");
        } catch (IOException problem) {
            throw invalid("", "CONFIG_READ_FAILED", kind + " could not be read");
        }
    }

    static String required(JsonNode root, String name) {
        JsonNode value = root.path(name);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw invalid(name, "FIELD_REQUIRED", name + " must be a non-blank string");
        }
        return value.textValue().trim();
    }

    static String optional(JsonNode root, String name, String fallback) {
        JsonNode value = root.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isTextual()) throw invalid(name, "INVALID_FIELD_TYPE", name + " must be a string");
        return value.textValue().trim();
    }

    static int integer(JsonNode root, String name, int fallback, int minimum, int maximum) {
        JsonNode value = root.path(name);
        int parsed = value.isMissingNode() || value.isNull() ? fallback
                : value.canConvertToInt() ? value.intValue() : Integer.MIN_VALUE;
        if (parsed < minimum || parsed > maximum) {
            throw invalid(name, "INVALID_NUMBER", name + " must be between " + minimum + " and " + maximum);
        }
        return parsed;
    }

    static long longValue(JsonNode root, String name, long fallback, long minimum, long maximum) {
        JsonNode value = root.path(name);
        long parsed = value.isMissingNode() || value.isNull() ? fallback
                : value.canConvertToLong() ? value.longValue() : Long.MIN_VALUE;
        if (parsed < minimum || parsed > maximum) {
            throw invalid(name, "INVALID_NUMBER", name + " must be between " + minimum + " and " + maximum);
        }
        return parsed;
    }

    static boolean bool(JsonNode root, String name, boolean fallback) {
        JsonNode value = root.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isBoolean()) throw invalid(name, "INVALID_FIELD_TYPE", name + " must be boolean");
        return value.booleanValue();
    }

    static SecretRef secret(JsonNode root, String name) {
        JsonNode value = root.path(name);
        if (!value.isObject()) throw invalid(name, "SECRET_REQUIRED", name + " must be a SecretRef");
        return new SecretRef(required(value, "namespace"), required(value, "name"), required(value, "version"));
    }

    static ConfigDocument schema(Class<?> owner, CarrierProtocol protocol, String kind) {
        String path = "/META-INF/wepush/providers/" + protocol.name().toLowerCase() + "/" + kind + ".schema.json";
        try (InputStream input = owner.getResourceAsStream(path)) {
            if (input == null) throw new ExceptionInInitializerError("Missing schema " + path);
            return new ConfigDocument(protocol.providerId() + "/" + kind, "1", input.readAllBytes());
        } catch (IOException problem) {
            throw new ExceptionInInitializerError(problem);
        }
    }

    static String recipient(RecipientRecord recipient, String name) {
        RecipientValue value = recipient.fields().get(name);
        String text = value instanceof RecipientValue.TextValue item ? item.value().trim() : "";
        if (text.isBlank()) throw invalid("recipient." + name, "RECIPIENT_FIELD_REQUIRED", name + " is required");
        return text;
    }

    static String render(String template, RecipientRecord recipient) {
        Matcher matcher = VARIABLE.matcher(template == null ? "" : template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            RecipientValue value = recipient.fields().get(matcher.group(1));
            String replacement = switch (value) {
                case RecipientValue.TextValue text -> text.value();
                case RecipientValue.NumberValue number -> number.value().toPlainString();
                case RecipientValue.BooleanValue bool -> Boolean.toString(bool.value());
                case null, default -> "";
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static CarrierProviderProblem invalid(String path, String code, String message) {
        return new CarrierProviderProblem(path, code, message);
    }
}

package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StandardProviderSupport {
    static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");

    private StandardProviderSupport() { }

    static JsonNode object(ConfigDocument document, String kind) {
        if (!ConfigDocument.JSON_MEDIA_TYPE.equals(document.mediaType())) {
            throw invalid("", "UNSUPPORTED_MEDIA_TYPE", kind + " configuration must use application/json");
        }
        try {
            JsonNode value = JSON.readTree(document.canonicalContent());
            if (value == null || !value.isObject()) {
                throw invalid("", "INVALID_JSON_OBJECT", kind + " configuration must be a JSON object");
            }
            return value;
        } catch (JsonProcessingException problem) {
            throw invalid("", "INVALID_JSON", kind + " configuration is not valid JSON");
        } catch (IOException problem) {
            throw invalid("", "CONFIG_READ_FAILED", kind + " configuration could not be read");
        }
    }

    static String requiredText(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw invalid(name, "FIELD_REQUIRED", name + " must be a non-blank string");
        }
        return value.textValue().trim();
    }

    static String optionalText(JsonNode node, String name, String fallback) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isTextual()) {
            throw invalid(name, "INVALID_FIELD_TYPE", name + " must be a string");
        }
        return value.textValue();
    }

    static boolean optionalBoolean(JsonNode node, String name, boolean fallback) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isBoolean()) throw invalid(name, "INVALID_FIELD_TYPE", name + " must be a boolean");
        return value.booleanValue();
    }

    static int integer(JsonNode node, String name, int fallback, int minimum, int maximum) {
        JsonNode value = node.path(name);
        int parsed = value.isMissingNode() || value.isNull() ? fallback
                : value.canConvertToInt() ? value.intValue() : Integer.MIN_VALUE;
        if (parsed < minimum || parsed > maximum) {
            throw invalid(name, "INVALID_NUMBER", name + " must be between " + minimum + " and " + maximum);
        }
        return parsed;
    }

    static Duration duration(JsonNode node, String name, Duration fallback, Duration maximum) {
        String value = optionalText(node, name, fallback.toString());
        try {
            Duration duration = Duration.parse(value);
            if (duration.isZero() || duration.isNegative() || duration.compareTo(maximum) > 0) {
                throw new DateTimeParseException("out of range", value, 0);
            }
            return duration;
        } catch (DateTimeParseException problem) {
            throw invalid(name, "INVALID_DURATION", name + " must be a positive ISO-8601 duration up to " + maximum);
        }
    }

    static <E extends Enum<E>> E enumeration(JsonNode node, String name, Class<E> type, E fallback) {
        String raw = optionalText(node, name, fallback.name()).trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException problem) {
            throw invalid(name, "UNSUPPORTED_VALUE", name + " is not supported");
        }
    }

    static SecretRef requiredSecret(JsonNode node, String name) {
        SecretRef ref = optionalSecret(node, name);
        if (ref == null) throw invalid(name, "SECRET_REQUIRED", name + " must be a SecretRef");
        return ref;
    }

    static SecretRef optionalSecret(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return null;
        if (!value.isObject()) throw invalid(name, "INVALID_SECRET_REF", name + " must be a SecretRef object");
        return new SecretRef(requiredText(value, "namespace"), requiredText(value, "name"),
                requiredText(value, "version"));
    }

    static List<String> stringList(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return List.of();
        if (!value.isArray()) throw invalid(name, "INVALID_LIST", name + " must be an array of strings");
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || item.textValue().isBlank()) {
                throw invalid(name, "INVALID_LIST_ITEM", name + " must contain non-blank strings");
            }
            result.add(item.textValue().trim());
        }
        return List.copyOf(result);
    }

    static String recipientText(RecipientRecord recipient, String name, boolean required) {
        RecipientValue value = recipient.fields().get(name);
        String text = text(value);
        if (required && text.isBlank()) {
            throw invalid("recipient." + name, "RECIPIENT_FIELD_REQUIRED",
                    "Recipient field " + name + " must be a non-blank string");
        }
        return text;
    }

    static String renderText(String template, RecipientRecord recipient) {
        return render(template, recipient, false);
    }

    static String renderJson(String template, RecipientRecord recipient) {
        String rendered = render(template, recipient, true);
        try {
            JsonNode parsed = JSON.readTree(rendered);
            if (parsed == null) throw new JsonProcessingException("empty JSON") { };
            return JSON.writeValueAsString(parsed);
        } catch (JsonProcessingException problem) {
            throw invalid("message", "RENDERED_JSON_INVALID", "Rendered message must be valid JSON");
        }
    }

    private static String render(String template, RecipientRecord recipient, boolean jsonEscape) {
        Matcher matcher = VARIABLE.matcher(template == null ? "" : template);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String replacement = text(recipient.fields().get(matcher.group(1)));
            if (jsonEscape) replacement = jsonEscape(replacement);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String text(RecipientValue value) {
        return switch (value) {
            case RecipientValue.TextValue text -> text.value();
            case RecipientValue.NumberValue number -> number.value().toPlainString();
            case RecipientValue.BooleanValue bool -> Boolean.toString(bool.value());
            case RecipientValue.NullValue ignored -> "";
            case RecipientValue.BinaryRefValue ignored -> "";
            case null -> "";
        };
    }

    private static String jsonEscape(String value) {
        try {
            String quoted = JSON.writeValueAsString(value);
            return quoted.substring(1, quoted.length() - 1);
        } catch (JsonProcessingException problem) {
            throw new IllegalStateException(problem);
        }
    }

    static ValidationResult validation(Runnable parser) {
        try {
            parser.run();
            return ValidationResult.valid();
        } catch (ProviderConfigException problem) {
            return ValidationResult.invalid(problem.path(), problem.code(), problem.getMessage());
        }
    }

    static ConfigDocument schema(Class<?> owner, String providerId, String slug, String kind) {
        String resource = "/META-INF/wepush/providers/" + slug + "/" + kind + ".schema.json";
        try (InputStream input = owner.getResourceAsStream(resource)) {
            if (input == null) throw new ExceptionInInitializerError("Missing provider schema: " + resource);
            return new ConfigDocument(providerId + "/" + kind, "1", ConfigDocument.JSON_MEDIA_TYPE,
                    input.readAllBytes());
        } catch (IOException problem) {
            throw new ExceptionInInitializerError(problem);
        }
    }

    static ProviderConfigException invalid(String path, String code, String message) {
        return new ProviderConfigException(path, code, message);
    }

    static String safeCode(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        String safe = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return safe.length() > 80 ? safe.substring(0, 80) : safe;
    }

    static byte[] utf8(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    static Map<String, String> metadata(String key, Object value) {
        return Map.of(key, String.valueOf(value));
    }
}

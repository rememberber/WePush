package com.fangxuele.wepush.next.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretRef;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HttpProviderConfig {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "authorization", "proxy-authorization", "connection", "content-length",
            "expect", "host", "upgrade", "transfer-encoding");

    private HttpProviderConfig() {
    }

    static Account parseAccount(ConfigDocument document) {
        JsonNode root = parse(document, "account");
        URI baseUrl;
        try {
            baseUrl = URI.create(requiredText(root, "baseUrl"));
        } catch (IllegalArgumentException exception) {
            throw invalid("baseUrl", "INVALID_URI", "baseUrl must be an absolute HTTP(S) URI");
        }
        String scheme = baseUrl.getScheme() == null
                ? "" : baseUrl.getScheme().toLowerCase(Locale.ROOT);
        if (!(scheme.equals("http") || scheme.equals("https"))
                || baseUrl.getHost() == null || baseUrl.getUserInfo() != null) {
            throw invalid("baseUrl", "INVALID_BASE_URL", "baseUrl must be an absolute HTTP(S) URI without user info");
        }

        Duration connectTimeout = duration(root, "connectTimeout", Duration.ofSeconds(5));
        Map<String, String> headers = stringMap(root.path("defaultHeaders"), "defaultHeaders");
        boolean allowPrivate = root.path("allowPrivateAddresses").asBoolean(false);
        Auth auth = parseAuth(root.path("auth"));
        return new Account(baseUrl, headers, auth, connectTimeout, allowPrivate);
    }

    static Message parseMessage(ConfigDocument document) {
        JsonNode root = parse(document, "message");
        String method = requiredText(root, "method").toUpperCase(Locale.ROOT);
        if (!METHODS.contains(method)) {
            throw invalid("method", "UNSUPPORTED_METHOD", "method must be GET, POST, PUT, PATCH or DELETE");
        }
        String path = requiredText(root, "path");
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("://")
                || path.contains("?") || path.contains("#")) {
            throw invalid("path", "INVALID_PATH", "path must be an origin-relative path");
        }
        Map<String, String> headers = stringMap(root.path("headers"), "headers");
        Map<String, String> query = stringMap(root.path("query"), "query");
        String bodyTemplate = optionalText(root, "bodyTemplate", "");
        if ((method.equals("GET") || method.equals("DELETE")) && !bodyTemplate.isEmpty()) {
            throw invalid("bodyTemplate", "BODY_NOT_ALLOWED", method + " does not accept a body in this provider");
        }

        Set<Integer> successStatuses = integerSet(root.path("successStatuses"));
        if (successStatuses.isEmpty()) {
            successStatuses = Set.of(200, 201, 202, 204);
        }
        String idempotencyHeader = optionalText(root, "idempotencyHeader", "");
        if (!idempotencyHeader.isEmpty()) {
            validateHeader(idempotencyHeader, "idempotencyHeader");
        }
        int responseLimitBytes = root.path("responseLimitBytes").asInt(1_048_576);
        if (responseLimitBytes < 1 || responseLimitBytes > 10 * 1_048_576) {
            throw invalid(
                    "responseLimitBytes", "INVALID_RESPONSE_LIMIT",
                    "responseLimitBytes must be between 1 and 10485760");
        }
        return new Message(
                method, path, headers, query, bodyTemplate, successStatuses,
                idempotencyHeader, responseLimitBytes);
    }

    private static Auth parseAuth(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return new Auth(AuthType.NONE, null);
        }
        if (!node.isObject()) {
            throw invalid("auth", "INVALID_AUTH", "auth must be an object");
        }
        String typeValue = optionalText(node, "type", "NONE").toUpperCase(Locale.ROOT);
        AuthType type;
        try {
            type = AuthType.valueOf(typeValue);
        } catch (IllegalArgumentException exception) {
            throw invalid("auth.type", "UNSUPPORTED_AUTH", "only NONE and BEARER authentication are supported");
        }
        if (type == AuthType.NONE) {
            return new Auth(type, null);
        }
        JsonNode secret = node.path("secret");
        if (!secret.isObject()) {
            throw invalid("auth.secret", "SECRET_REQUIRED", "bearer authentication requires a SecretRef");
        }
        return new Auth(type, new SecretRef(
                requiredText(secret, "namespace"),
                requiredText(secret, "name"),
                requiredText(secret, "version")));
    }

    private static JsonNode parse(ConfigDocument document, String kind) {
        if (!ConfigDocument.JSON_MEDIA_TYPE.equals(document.mediaType())) {
            throw invalid("", "UNSUPPORTED_MEDIA_TYPE", kind + " configuration must use application/json");
        }
        try {
            JsonNode root = JSON.readTree(document.canonicalContent());
            if (root == null || !root.isObject()) {
                throw invalid("", "INVALID_JSON_OBJECT", kind + " configuration must be a JSON object");
            }
            return root;
        } catch (IOException exception) {
            throw invalid("", "INVALID_JSON", kind + " configuration is not valid JSON");
        }
    }

    private static String requiredText(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw invalid(name, "FIELD_REQUIRED", name + " must be a non-blank string");
        }
        return value.textValue();
    }

    private static String optionalText(JsonNode node, String name, String defaultValue) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        if (!value.isTextual()) {
            throw invalid(name, "INVALID_FIELD_TYPE", name + " must be a string");
        }
        return value.textValue();
    }

    private static Duration duration(JsonNode node, String name, Duration defaultValue) {
        String value = optionalText(node, name, defaultValue.toString());
        try {
            Duration duration = Duration.parse(value);
            if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofMinutes(2)) > 0) {
                throw new DateTimeParseException("out of range", value, 0);
            }
            return duration;
        } catch (DateTimeParseException exception) {
            throw invalid(name, "INVALID_DURATION", name + " must be a positive ISO-8601 duration up to PT2M");
        }
    }

    private static Map<String, String> stringMap(JsonNode node, String path) {
        if (node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        if (!node.isObject()) {
            throw invalid(path, "INVALID_MAP", path + " must be an object of string values");
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.properties().forEach(entry -> {
            if (!entry.getValue().isTextual()) {
                throw invalid(path + "." + entry.getKey(), "INVALID_MAP_VALUE", "header and query values must be strings");
            }
            if (path.toLowerCase(Locale.ROOT).contains("header")) {
                validateHeader(entry.getKey(), path + "." + entry.getKey());
                String headerValue = entry.getValue().textValue();
                if (headerValue.chars().anyMatch(character -> character == '\r' || character == '\n')) {
                    throw invalid(path + "." + entry.getKey(), "INVALID_HEADER_VALUE", "header value contains a line break");
                }
            }
            values.put(entry.getKey(), entry.getValue().textValue());
        });
        return Map.copyOf(values);
    }

    private static Set<Integer> integerSet(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return Set.of();
        }
        if (!node.isArray()) {
            throw invalid("successStatuses", "INVALID_STATUS_LIST", "successStatuses must be an array");
        }
        List<Integer> statuses = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.canConvertToInt() || value.intValue() < 100 || value.intValue() > 599) {
                throw invalid("successStatuses", "INVALID_STATUS", "HTTP status must be between 100 and 599");
            }
            statuses.add(value.intValue());
        }
        return Set.copyOf(statuses);
    }

    private static void validateHeader(String name, String path) {
        if (!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                || FORBIDDEN_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            throw invalid(path, "FORBIDDEN_HEADER", "header is invalid or controlled by the HTTP client");
        }
    }

    private static ConfigException invalid(String path, String code, String message) {
        return new ConfigException(path, code, message);
    }

    record Account(
            URI baseUrl,
            Map<String, String> defaultHeaders,
            Auth auth,
            Duration connectTimeout,
            boolean allowPrivateAddresses
    ) {
    }

    record Message(
            String method,
            String path,
            Map<String, String> headers,
            Map<String, String> query,
            String bodyTemplate,
            Set<Integer> successStatuses,
            String idempotencyHeader,
            int responseLimitBytes
    ) {
    }

    record Auth(AuthType type, SecretRef secretRef) {
    }

    enum AuthType {
        NONE,
        BEARER
    }

    static final class ConfigException extends IllegalArgumentException {
        private final String path;
        private final String code;

        private ConfigException(String path, String code, String message) {
            super(message);
            this.path = path;
            this.code = code;
        }

        String path() {
            return path;
        }

        String code() {
            return code;
        }
    }
}

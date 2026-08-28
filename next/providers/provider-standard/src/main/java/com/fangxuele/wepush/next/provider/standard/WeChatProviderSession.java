package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fangxuele.wepush.next.core.api.CancellationToken;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

final class WeChatProviderSession implements ProviderSession {
    private static final int MAX_RESPONSE_BYTES = 65_536;
    private static final int MAX_PAYLOAD_BYTES = 256 * 1024;

    private final WeChatPlatform platform;
    private final WeChatProviderConfig.Account account;
    private final WeChatProviderConfig.Message message;
    private final SecretResolver secrets;
    private final ExecutionClock clock;
    private final boolean dryRun;
    private final URI apiBase;
    private final HttpClient client;
    private CachedToken cachedToken;

    WeChatProviderSession(WeChatPlatform platform, WeChatProviderConfig.Account account,
                          WeChatProviderConfig.Message message, SecretResolver secrets,
                          ExecutionClock clock, boolean dryRun, URI apiBase) {
        this.platform = platform;
        this.account = account;
        this.message = message;
        this.secrets = secrets;
        this.clock = clock;
        this.dryRun = dryRun;
        this.apiBase = apiBase;
        client = HttpClient.newBuilder().connectTimeout(account.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public ProviderResult send(ProviderSendRequest request, CancellationToken token) {
        if (token.cancelled()) return failure("CANCELLED", ErrorCategory.CANCELLED, false,
                "Message send was cancelled before request");
        if (!request.deadline().isAfter(clock.now())) return failure("ITEM_TIMEOUT",
                ErrorCategory.TIMEOUT, false, "Message deadline elapsed before request");
        boolean submitted = false;
        try {
            ObjectNode payload = payload(request);
            byte[] bytes = StandardProviderSupport.JSON.writeValueAsBytes(payload);
            if (bytes.length > MAX_PAYLOAD_BYTES) return failure("PAYLOAD_TOO_LARGE",
                    ErrorCategory.INVALID_REQUEST, false, "Message payload exceeds 256 KiB");
            if (dryRun) return ProviderResult.success("DRY_RUN", "");

            Duration timeout = remaining(request);
            String accessToken = accessToken(false, timeout);
            submitted = true;
            ApiResponse response = post(accessToken, bytes, timeout);
            JsonNode responseBody = response.json();
            if (response.status() >= 200 && response.status() < 300
                    && invalidToken(responseBody.path("errcode").asInt(Integer.MIN_VALUE))) {
                submitted = false;
                accessToken = accessToken(true, remaining(request));
                submitted = true;
                response = post(accessToken, bytes, remaining(request));
                responseBody = response.json();
            }
            return classify(response, responseBody);
        } catch (ProviderConfigException problem) {
            return failure(problem.code(), problem.path().startsWith("recipient.")
                    ? ErrorCategory.RECIPIENT_INVALID : ErrorCategory.INVALID_REQUEST,
                    false, problem.getMessage());
        } catch (RemoteFailure problem) {
            return submitted ? unknown(problem.code(), ErrorCategory.UNKNOWN,
                    "Message outcome is unknown because the remote response could not be interpreted")
                    : failure(problem.code(), problem.category(), problem.retryable(), problem.diagnostic());
        } catch (HttpTimeoutException problem) {
            return submitted ? unknown("MESSAGE_TIMEOUT", ErrorCategory.TIMEOUT,
                    "Message outcome is unknown after timeout")
                    : failure("TOKEN_TIMEOUT", ErrorCategory.TIMEOUT, true,
                    "Access token request timed out before message submission");
        } catch (ConnectException problem) {
            return submitted ? unknown("MESSAGE_CONNECT_UNKNOWN", ErrorCategory.NETWORK,
                    "Message outcome is unknown after connection failure")
                    : failure("TOKEN_CONNECT_FAILED", ErrorCategory.NETWORK, true,
                    "Access token endpoint connection failed before message submission");
        } catch (IOException problem) {
            return submitted ? unknown("MESSAGE_IO_UNKNOWN", ErrorCategory.NETWORK,
                    "Message outcome is unknown after I/O failure")
                    : failure("TOKEN_IO_FAILED", ErrorCategory.NETWORK, true,
                    "Access token request failed before message submission");
        } catch (InterruptedException problem) {
            Thread.currentThread().interrupt();
            return submitted ? unknown("MESSAGE_INTERRUPTED", ErrorCategory.CANCELLED,
                    "Message outcome is unknown after interruption")
                    : failure("TOKEN_INTERRUPTED", ErrorCategory.CANCELLED, false,
                    "Interrupted before message submission");
        } catch (RuntimeException problem) {
            return failure("WECHAT_REQUEST_INVALID", ErrorCategory.INVALID_REQUEST, false,
                    problem.getClass().getSimpleName());
        }
    }

    private ObjectNode payload(ProviderSendRequest request) {
        JsonNode rendered;
        try {
            rendered = StandardProviderSupport.JSON.readTree(StandardProviderSupport.renderJson(
                    message.payloadJsonTemplate(), request.recipient()));
        } catch (IOException problem) {
            throw StandardProviderSupport.invalid("payloadJsonTemplate", "RENDERED_JSON_INVALID",
                    "Rendered message payload is invalid JSON");
        }
        if (!rendered.isObject()) throw StandardProviderSupport.invalid("payloadJsonTemplate",
                "PAYLOAD_OBJECT_REQUIRED", "Rendered message payload must be a JSON object");
        ObjectNode payload = (ObjectNode) rendered;
        if (platform == WeChatPlatform.WECOM_APP) {
            String userId = StandardProviderSupport.recipientText(request.recipient(), "userId", false);
            String partyId = StandardProviderSupport.recipientText(request.recipient(), "partyId", false);
            String tagId = StandardProviderSupport.recipientText(request.recipient(), "tagId", false);
            if (userId.isBlank() && partyId.isBlank() && tagId.isBlank()) {
                throw StandardProviderSupport.invalid("recipient.userId", "WECOM_RECIPIENT_REQUIRED",
                        "At least one of userId, partyId, or tagId is required");
            }
            if (!payload.path("msgtype").isTextual() || payload.path("msgtype").asText().isBlank()) {
                throw StandardProviderSupport.invalid("payloadJsonTemplate", "WECOM_MSGTYPE_REQUIRED",
                        "WeCom application payload must contain msgtype");
            }
            setOrRemove(payload, "touser", userId);
            setOrRemove(payload, "toparty", partyId);
            setOrRemove(payload, "totag", tagId);
            payload.put("agentid", account.agentId());
            payload.put("enable_duplicate_check", message.enableDuplicateCheck() ? 1 : 0);
            payload.put("duplicate_check_interval", message.duplicateCheckInterval());
        } else {
            payload.put("touser", StandardProviderSupport.recipientText(
                    request.recipient(), "openId", true));
        }
        return payload;
    }

    private static void setOrRemove(ObjectNode payload, String name, String value) {
        if (value.isBlank()) payload.remove(name); else payload.put(name, value);
    }

    private synchronized String accessToken(boolean force, Duration timeout)
            throws IOException, InterruptedException, RemoteFailure {
        if (!force && cachedToken != null && cachedToken.validUntil().isAfter(clock.now())) {
            return cachedToken.value();
        }
        cachedToken = fetchAccessToken(platform, account, secrets, clock, client, apiBase, timeout);
        return cachedToken.value();
    }

    static CachedToken fetchAccessToken(WeChatPlatform platform, WeChatProviderConfig.Account account,
                                        SecretResolver secrets, ExecutionClock clock, HttpClient client,
                                        URI apiBase, Duration timeout)
            throws IOException, InterruptedException, RemoteFailure {
        String secret = AbstractBotProviderFactory.resolve(secrets, account.credential());
        String query = platform == WeChatPlatform.WECOM_APP
                ? "corpid=" + encode(account.principalId()) + "&corpsecret=" + encode(secret)
                : "grant_type=client_credential&appid=" + encode(account.principalId())
                + "&secret=" + encode(secret);
        URI uri = endpoint(apiBase, platform.tokenPath(), query);
        HttpResponse<InputStream> raw = client.send(HttpRequest.newBuilder(uri).timeout(timeout).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        ApiResponse response = read(raw);
        if (response.status() == 429) throw new RemoteFailure("TOKEN_HTTP_429",
                ErrorCategory.RATE_LIMITED, true, "Access token endpoint rate limit reached");
        if (response.status() >= 500) throw new RemoteFailure("TOKEN_HTTP_" + response.status(),
                ErrorCategory.TEMPORARY_REMOTE, true, "Access token service is temporarily unavailable");
        if (response.status() < 200 || response.status() >= 300) {
            throw new RemoteFailure("TOKEN_HTTP_" + response.status(), ErrorCategory.PERMANENT_REMOTE,
                    false, "Access token endpoint rejected the request");
        }
        JsonNode body = response.json();
        String token = body.path("access_token").asText("");
        if (token.isBlank()) {
            int rawCode = body.path("errcode").asInt(Integer.MIN_VALUE);
            ErrorCategory category = tokenCategory(rawCode);
            throw new RemoteFailure("TOKEN_" + safeRemoteCode(rawCode), category,
                    rawCode == -1 || rawCode == 45009,
                    category == ErrorCategory.AUTHENTICATION ? "Application credential was rejected"
                            : "Access token request was rejected");
        }
        long expiresIn = Math.max(60, Math.min(body.path("expires_in").asLong(7200), 86_400));
        return new CachedToken(token, clock.now().plusSeconds(Math.max(30, expiresIn - 300)));
    }

    private ApiResponse post(String token, byte[] payload, Duration timeout)
            throws IOException, InterruptedException, RemoteFailure {
        URI uri = endpoint(apiBase, platform.sendPath(message.type()), "access_token=" + encode(token));
        HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(uri).timeout(timeout)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(payload)).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        return read(response);
    }

    private ProviderResult classify(ApiResponse response, JsonNode body) {
        int status = response.status();
        if (status == 401) return failure("HTTP_401", ErrorCategory.AUTHENTICATION, false,
                "Application credential was rejected");
        if (status == 403) return failure("HTTP_403", ErrorCategory.AUTHORIZATION, false,
                "Message request was forbidden");
        if (status == 429) return rate("HTTP_429", retryAfter(response.headers()));
        if (status >= 500) return unknown("HTTP_" + status, ErrorCategory.TEMPORARY_REMOTE,
                "Message outcome is unknown after remote server error");
        if (status < 200 || status >= 300) return failure("HTTP_" + status,
                ErrorCategory.PERMANENT_REMOTE, false, "Remote API rejected the HTTP request");
        int rawCode = body.path("errcode").asInt(Integer.MIN_VALUE);
        String code = platform.name() + "_" + safeRemoteCode(rawCode);
        if (rawCode == 0) {
            String requestId = body.path("msgid").asText("");
            if (requestId.isBlank()) requestId = response.headers().firstValue("x-request-id").orElse("");
            return new ProviderResult(ItemState.SUCCEEDED, platform.name() + "_OK",
                    ErrorCategory.NONE, false, null, "", requestId, Map.of());
        }
        if (rawCode == -1) return failure(code, ErrorCategory.TEMPORARY_REMOTE, true,
                "Remote API is temporarily busy");
        if (rawCode == 45009 || rawCode == 45047 || rawCode == 45011) {
            return rate(code, Duration.ofMinutes(1));
        }
        if (rawCode == 40001 || rawCode == 40013) return failure(code,
                ErrorCategory.AUTHENTICATION, false, "Application credential was rejected");
        if (invalidToken(rawCode)) return failure(code, ErrorCategory.AUTHENTICATION, false,
                "Access token remained invalid after one safe refresh");
        if (rawCode == 40164 || rawCode == 48001 || rawCode == 60020) return failure(code,
                ErrorCategory.AUTHORIZATION, false, "Application permission or IP allowlist rejected the request");
        if (rawCode == 40003 || rawCode == 43004 || rawCode == 60111 || rawCode == 81013) {
            return failure(code, ErrorCategory.RECIPIENT_INVALID, false, "Recipient was rejected");
        }
        if (rawCode == 40037 || rawCode == 47003 || rawCode == 41030 || rawCode == 40032) {
            return failure(code, ErrorCategory.INVALID_REQUEST, false, "Message template or payload was rejected");
        }
        return failure(code, ErrorCategory.PERMANENT_REMOTE, false, "Remote API rejected the message");
    }

    private Duration remaining(ProviderSendRequest request) {
        Duration duration = Duration.between(clock.now(), request.deadline());
        if (duration.isZero() || duration.isNegative()) {
            throw StandardProviderSupport.invalid("deadline", "ITEM_TIMEOUT", "Message deadline elapsed");
        }
        return duration;
    }

    private static boolean invalidToken(int code) {
        return code == 40014 || code == 42001 || code == 42007 || code == 42009;
    }

    private static ErrorCategory tokenCategory(int code) {
        if (code == 40001 || code == 40013 || code == 40125) return ErrorCategory.AUTHENTICATION;
        if (code == 40164 || code == 60020) return ErrorCategory.AUTHORIZATION;
        if (code == 45009) return ErrorCategory.RATE_LIMITED;
        if (code == -1) return ErrorCategory.TEMPORARY_REMOTE;
        return ErrorCategory.PERMANENT_REMOTE;
    }

    private static String safeRemoteCode(int code) {
        return code == Integer.MIN_VALUE ? "UNKNOWN" : Integer.toString(code).replace('-', 'N');
    }

    private static URI endpoint(URI base, String path, String query) {
        String root = base.toASCIIString();
        while (root.endsWith("/")) root = root.substring(0, root.length() - 1);
        return URI.create(root + path + "?" + query);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static ApiResponse read(HttpResponse<InputStream> response) throws IOException, RemoteFailure {
        byte[] bytes;
        try (InputStream input = response.body()) { bytes = input.readNBytes(MAX_RESPONSE_BYTES + 1); }
        if (bytes.length > MAX_RESPONSE_BYTES) throw new RemoteFailure("WECHAT_RESPONSE_TOO_LARGE",
                ErrorCategory.PERMANENT_REMOTE, false, "Remote response exceeded 64 KiB");
        JsonNode body;
        try { body = StandardProviderSupport.JSON.readTree(bytes); }
        catch (IOException problem) { throw new RemoteFailure("WECHAT_RESPONSE_INVALID",
                ErrorCategory.PERMANENT_REMOTE, false, "Remote response was not valid JSON"); }
        if (body == null || !body.isObject()) throw new RemoteFailure("WECHAT_RESPONSE_INVALID",
                ErrorCategory.PERMANENT_REMOTE, false, "Remote response was not a JSON object");
        return new ApiResponse(response.statusCode(), response.headers(), body);
    }

    private static Duration retryAfter(HttpHeaders headers) {
        return headers.firstValue("retry-after").flatMap(value -> {
            try {
                long seconds = Long.parseLong(value);
                return seconds >= 0 && seconds <= 3600 ? java.util.Optional.of(Duration.ofSeconds(seconds))
                        : java.util.Optional.empty();
            } catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
        }).orElse(null);
    }

    private static ProviderResult rate(String code, Duration retryAfter) {
        return new ProviderResult(ItemState.FAILED, code, ErrorCategory.RATE_LIMITED,
                true, retryAfter, "Remote API rate limit reached", "", Map.of());
    }

    private static ProviderResult failure(String code, ErrorCategory category,
                                          boolean retryable, String diagnostic) {
        return ProviderResult.failure(code, category, retryable, diagnostic);
    }

    private static ProviderResult unknown(String code, ErrorCategory category, String diagnostic) {
        return new ProviderResult(ItemState.UNKNOWN, code, category, false,
                null, diagnostic, "", Map.of());
    }

    record CachedToken(String value, Instant validUntil) { }

    private record ApiResponse(int status, HttpHeaders headers, JsonNode json) { }

    static final class RemoteFailure extends Exception {
        private final String code;
        private final ErrorCategory category;
        private final boolean retryable;
        private final String diagnostic;

        RemoteFailure(String code, ErrorCategory category, boolean retryable, String diagnostic) {
            super(diagnostic);
            this.code = code;
            this.category = category;
            this.retryable = retryable;
            this.diagnostic = diagnostic;
        }

        String code() { return code; }
        ErrorCategory category() { return category; }
        boolean retryable() { return retryable; }
        String diagnostic() { return diagnostic; }
    }

    @Override
    public void close() { cachedToken = null; }
}

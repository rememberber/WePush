package com.fangxuele.wepush.next.provider.http;

import com.fangxuele.wepush.next.core.api.CancellationToken;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class HttpProviderSession implements ProviderSession {
    private final HttpProviderConfig.Account account;
    private final HttpProviderConfig.Message message;
    private final SecretResolver secrets;
    private final HttpClient client;
    private final boolean dryRun;

    HttpProviderSession(
            HttpProviderConfig.Account account,
            HttpProviderConfig.Message message,
            SecretResolver secrets,
            boolean dryRun
    ) {
        this.account = account;
        this.message = message;
        this.secrets = secrets;
        this.dryRun = dryRun;
        client = HttpClient.newBuilder()
                .connectTimeout(account.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public ProviderResult send(ProviderSendRequest request, CancellationToken token) {
        if (token.cancelled()) {
            return unknown("CANCELLED", ErrorCategory.CANCELLED, false, "Request was cancelled before send");
        }
        if (request.deadline().isBefore(Instant.now())) {
            return unknown("ITEM_TIMEOUT", ErrorCategory.TIMEOUT, false, "Item deadline has elapsed");
        }
        if (dryRun) {
            return ProviderResult.success("DRY_RUN", "");
        }
        if (request.messageConfig() == null) {
            return failure("MESSAGE_CONFIG_MISSING", ErrorCategory.INVALID_REQUEST, false, "Message configuration is missing");
        }

        try {
            URI target = targetUri(request);
            SsrfGuard.verify(target, account.allowPrivateAddresses());
            HttpRequest httpRequest = buildRequest(target, request);
            if (request.runId().isBlank()) {
                return failure("RUN_ID_MISSING", ErrorCategory.INVALID_REQUEST, false, "Run ID is missing");
            }
            HttpResponse<InputStream> response = client.send(
                    httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            return classify(response);
        } catch (SecurityException exception) {
            return failure("SSRF_BLOCKED", ErrorCategory.INVALID_REQUEST, false, "HTTP target is not allowed");
        } catch (UnknownHostException exception) {
            return failure("DNS_FAILED", ErrorCategory.NETWORK, true, "HTTP target could not be resolved");
        } catch (HttpTimeoutException exception) {
            return unknown(
                    "HTTP_TIMEOUT", ErrorCategory.TIMEOUT, idempotent(),
                    "HTTP response is unknown after timeout");
        } catch (ConnectException exception) {
            return failure("CONNECT_FAILED", ErrorCategory.NETWORK, true, "HTTP connection failed");
        } catch (IOException exception) {
            return unknown(
                    "HTTP_IO_FAILED", ErrorCategory.NETWORK, idempotent(),
                    "HTTP response is unknown after I/O failure");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unknown(
                    "HTTP_INTERRUPTED", ErrorCategory.CANCELLED, false,
                    "HTTP request was interrupted and its outcome may be unknown");
        } catch (IllegalArgumentException exception) {
            return failure(
                    "REQUEST_BUILD_FAILED", ErrorCategory.INVALID_REQUEST, false,
                    "HTTP request configuration is invalid");
        }
    }

    @Override
    public void close() {
        // java.net.http.HttpClient has no close operation on Java 21.
    }

    private URI targetUri(ProviderSendRequest request) {
        URI resolved = account.baseUrl().resolve(message.path());
        if (!sameOrigin(account.baseUrl(), resolved)) {
            throw new SecurityException("message path changed origin");
        }
        if (message.query().isEmpty()) {
            return resolved;
        }
        StringBuilder query = new StringBuilder();
        message.query().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(TemplateRenderer.renderText(entry.getValue(), request.recipient())));
        });
        return URI.create(resolved.toASCIIString() + "?" + query);
    }

    private HttpRequest buildRequest(URI target, ProviderSendRequest request) {
        Duration timeout = Duration.between(Instant.now(), request.deadline());
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("request deadline elapsed");
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(target).timeout(timeout);

        Map<String, String> headers = new LinkedHashMap<>(account.defaultHeaders());
        headers.putAll(message.headers());
        headers.forEach((name, value) -> builder.header(
                name, TemplateRenderer.renderText(value, request.recipient())));
        if (!message.idempotencyHeader().isEmpty()) {
            builder.header(message.idempotencyHeader(), request.idempotencyKey());
        }
        applyAuthentication(builder);

        HttpRequest.BodyPublisher body = message.bodyTemplate().isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(
                        TemplateRenderer.renderJsonStringContent(
                                message.bodyTemplate(), request.recipient()),
                        StandardCharsets.UTF_8);
        return builder.method(message.method(), body).build();
    }

    private void applyAuthentication(HttpRequest.Builder builder) {
        if (account.auth().type() == HttpProviderConfig.AuthType.NONE) {
            return;
        }
        try (SecretValue secret = secrets.resolve(account.auth().secretRef())) {
            if (secret == null) {
                throw new IllegalArgumentException("SecretResolver returned null");
            }
            char[] value = secret.copyChars();
            try {
                builder.header("Authorization", "Bearer " + new String(value));
            } finally {
                Arrays.fill(value, '\0');
            }
        }
    }

    private ProviderResult classify(HttpResponse<InputStream> response) throws IOException {
        byte[] body;
        try (InputStream input = response.body()) {
            body = input.readNBytes(message.responseLimitBytes() + 1);
        }
        if (body.length > message.responseLimitBytes()) {
            return failure(
                    "RESPONSE_TOO_LARGE", ErrorCategory.PERMANENT_REMOTE, false,
                    "HTTP response exceeded the configured size limit");
        }

        int status = response.statusCode();
        String requestId = response.headers().firstValue("x-request-id")
                .or(() -> response.headers().firstValue("request-id"))
                .orElse("");
        Map<String, String> metadata = Map.of(
                "httpStatus", Integer.toString(status),
                "responseBytes", Integer.toString(body.length));
        if (message.successStatuses().contains(status)) {
            return new ProviderResult(
                    com.fangxuele.wepush.next.core.api.ItemState.SUCCEEDED,
                    "HTTP_" + status,
                    ErrorCategory.NONE,
                    false,
                    null,
                    "",
                    requestId,
                    metadata);
        }
        if (status == 401) {
            return result("HTTP_401", ErrorCategory.AUTHENTICATION, false, requestId, metadata, null);
        }
        if (status == 403) {
            return result("HTTP_403", ErrorCategory.AUTHORIZATION, false, requestId, metadata, null);
        }
        if (status == 408) {
            return unknown("HTTP_408", ErrorCategory.TIMEOUT, idempotent(), "Remote request timed out");
        }
        if (status == 429) {
            return result(
                    "HTTP_429", ErrorCategory.RATE_LIMITED, true, requestId, metadata,
                    retryAfter(response));
        }
        if (status >= 500) {
            return result("HTTP_" + status, ErrorCategory.TEMPORARY_REMOTE, true, requestId, metadata, null);
        }
        return result("HTTP_" + status, ErrorCategory.PERMANENT_REMOTE, false, requestId, metadata, null);
    }

    private ProviderResult result(
            String code,
            ErrorCategory category,
            boolean retryable,
            String requestId,
            Map<String, String> metadata,
            Duration retryAfter
    ) {
        return new ProviderResult(
                com.fangxuele.wepush.next.core.api.ItemState.FAILED,
                code,
                category,
                retryable,
                retryAfter,
                "Remote HTTP endpoint returned " + code,
                requestId,
                metadata);
    }

    private Duration retryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("retry-after").flatMap(value -> {
            try {
                long seconds = Long.parseLong(value.trim());
                return seconds < 0 || seconds > 3600
                        ? java.util.Optional.empty()
                        : java.util.Optional.of(Duration.ofSeconds(seconds));
            } catch (NumberFormatException exception) {
                return java.util.Optional.empty();
            }
        }).orElse(null);
    }

    private boolean idempotent() {
        return !message.idempotencyHeader().isEmpty();
    }

    private static ProviderResult failure(
            String code,
            ErrorCategory category,
            boolean retryable,
            String diagnostic
    ) {
        return ProviderResult.failure(code, category, retryable, diagnostic);
    }

    private static ProviderResult unknown(
            String code,
            ErrorCategory category,
            boolean retryable,
            String diagnostic
    ) {
        return new ProviderResult(
                com.fangxuele.wepush.next.core.api.ItemState.UNKNOWN,
                code, category, retryable, null, diagnostic, "", Map.of());
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right)
                && right.getUserInfo() == null;
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

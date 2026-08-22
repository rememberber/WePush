package com.fangxuele.wepush.next.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

final class HttpTransport implements AutoCloseable {
    private final URI endpoint;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final TokenProvider tokenProvider;
    private final Duration requestTimeout;
    private final RetryPolicy retryPolicy;

    HttpTransport(
            URI endpoint,
            HttpClient client,
            ObjectMapper mapper,
            TokenProvider tokenProvider,
            Duration requestTimeout,
            RetryPolicy retryPolicy
    ) {
        String normalized = endpoint.toString().replaceAll("/+$", "");
        this.endpoint = URI.create(normalized);
        this.client = client;
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
        this.requestTimeout = requestTimeout;
        this.retryPolicy = retryPolicy;
    }

    URI endpoint() {
        return endpoint;
    }

    <T> T getJson(String path, Class<T> responseType) {
        String body = getText(path);
        return decode(body, responseType);
    }

    InputStream getStream(String path) {
        for (int attempt = 1; attempt <= retryPolicy.maximumAttempts(); attempt++) {
            HttpRequest request = requestBuilder(path)
                    .header("Accept", "application/octet-stream, text/csv")
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response = client.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
                try (InputStream errorBody = response.body()) {
                    String body = new String(errorBody.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (retryable(response.statusCode()) && attempt < retryPolicy.maximumAttempts()) {
                        sleep(retryDelay(response, attempt));
                        continue;
                    }
                    throw new WePushException(
                            "WePush Service returned HTTP " + response.statusCode(),
                            response.statusCode(), body);
                }
            } catch (IOException exception) {
                if (attempt >= retryPolicy.maximumAttempts()) {
                    throw new WePushException("Unable to reach WePush Service", exception);
                }
                sleep(withJitter(retryPolicy.delayForAttempt(attempt)));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new WePushException("Interrupted while calling WePush Service", exception);
            }
        }
        throw new IllegalStateException("unreachable retry state");
    }

    <T> T postJson(String path, Object requestBody, String idempotencyKey, Class<T> responseType) {
        return writeJson(path, requestBody, idempotencyKey, "POST", responseType);
    }

    <T> T putJson(String path, Object requestBody, Class<T> responseType) {
        return writeJson(path, requestBody, null, "PUT", responseType);
    }

    private <T> T writeJson(String path, Object requestBody, String idempotencyKey,
                            String method, Class<T> responseType) {
        String body;
        try {
            body = mapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException exception) {
            throw new WePushException("Unable to encode request JSON", exception);
        }
        int attempts = idempotencyKey == null ? 1 : retryPolicy.maximumAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            HttpRequest.Builder builder = requestBuilder(path)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
            if (idempotencyKey != null) {
                builder.header("Idempotency-Key", safeHeader(idempotencyKey, "idempotencyKey"));
            }
            try {
                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return decode(response.body(), responseType);
                }
                if (retryable(response.statusCode()) && attempt < attempts) {
                    sleep(retryDelay(response, attempt));
                    continue;
                }
                throw new WePushException("WePush Service returned HTTP " + response.statusCode(),
                        response.statusCode(), response.body());
            } catch (IOException exception) {
                if (attempt >= attempts) {
                    throw new WePushException("Unable to reach WePush Service", exception);
                }
                sleep(withJitter(retryPolicy.delayForAttempt(attempt)));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new WePushException("Interrupted while calling WePush Service", exception);
            }
        }
        throw new IllegalStateException("unreachable retry state");
    }

    String getText(String path) {
        for (int attempt = 1; attempt <= retryPolicy.maximumAttempts(); attempt++) {
            HttpRequest request = requestBuilder(path)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
                if (retryable(response.statusCode()) && attempt < retryPolicy.maximumAttempts()) {
                    sleep(retryDelay(response, attempt));
                    continue;
                }
                throw new WePushException(
                        "WePush Service returned HTTP " + response.statusCode(),
                        response.statusCode(),
                        response.body());
            } catch (IOException exception) {
                if (attempt >= retryPolicy.maximumAttempts()) {
                    throw new WePushException("Unable to reach WePush Service", exception);
                }
                sleep(withJitter(retryPolicy.delayForAttempt(attempt)));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new WePushException("Interrupted while calling WePush Service", exception);
            }
        }
        throw new IllegalStateException("unreachable retry state");
    }

    private HttpRequest.Builder requestBuilder(String path) {
        URI target = path.startsWith("http://") || path.startsWith("https://")
                ? URI.create(path)
                : URI.create(endpoint + (path.startsWith("/") ? path : "/" + path));
        HttpRequest.Builder builder = HttpRequest.newBuilder(target)
                .timeout(requestTimeout)
                .header("User-Agent", "wepush-next-java-sdk/0.1");
        String token = tokenProvider.currentToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + safeHeader(token, "token"));
        }
        return builder;
    }

    private <T> T decode(String body, Class<T> responseType) {
        try {
            return mapper.readValue(body, responseType);
        } catch (JsonProcessingException exception) {
            throw new WePushException("Service returned invalid JSON", exception);
        }
    }

    private static String safeHeader(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(name + " is blank or contains prohibited control characters");
        }
        return value;
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(HttpTransport::parseRetryAfter)
                .map(delay -> min(delay, retryPolicy.maximumDelay()))
                .orElseGet(() -> withJitter(retryPolicy.delayForAttempt(attempt)));
    }

    private static Duration parseRetryAfter(String value) {
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            try {
                Duration delay = Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(
                        value, DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)));
                return delay.isNegative() ? Duration.ZERO : delay;
            } catch (DateTimeParseException invalidDate) {
                return Duration.ZERO;
            }
        }
    }

    private static Duration withJitter(Duration duration) {
        if (duration.isZero()) {
            return duration;
        }
        double multiplier = ThreadLocalRandom.current().nextDouble(0.8, 1.2);
        return Duration.ofMillis(Math.max(0L, Math.round(duration.toMillis() * multiplier)));
    }

    private static Duration min(Duration first, Duration second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private static boolean retryable(int statusCode) {
        return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WePushException("Interrupted during retry backoff", exception);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}

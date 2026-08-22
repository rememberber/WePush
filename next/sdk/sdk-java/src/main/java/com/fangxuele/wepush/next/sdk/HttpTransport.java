package com.fangxuele.wepush.next.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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
        try {
            return mapper.readValue(body, responseType);
        } catch (JsonProcessingException exception) {
            throw new WePushException("Service returned invalid JSON", exception);
        }
    }

    String getText(String path) {
        for (int attempt = 1; attempt <= retryPolicy.maximumAttempts(); attempt++) {
            HttpRequest request = request(path);
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

    private HttpRequest request(String path) {
        URI target = path.startsWith("http://") || path.startsWith("https://")
                ? URI.create(path)
                : URI.create(endpoint + (path.startsWith("/") ? path : "/" + path));
        HttpRequest.Builder builder = HttpRequest.newBuilder(target)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", "wepush-next-java-sdk/0.1")
                .GET();
        String token = tokenProvider.currentToken();
        if (token != null && !token.isBlank()) {
            if (token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("token contains prohibited control characters");
            }
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
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

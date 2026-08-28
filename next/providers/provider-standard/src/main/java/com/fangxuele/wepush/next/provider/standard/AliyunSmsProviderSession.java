package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.CancellationToken;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

final class AliyunSmsProviderSession implements ProviderSession {
    private final AliyunSmsProviderConfig.Account account;
    private final AliyunSmsProviderConfig.Message message;
    private final SecretResolver secrets;
    private final ExecutionClock clock;
    private final boolean dryRun;
    private final URI endpoint;
    private final HttpClient client;

    AliyunSmsProviderSession(AliyunSmsProviderConfig.Account account,
                             AliyunSmsProviderConfig.Message message,
                             SecretResolver secrets, ExecutionClock clock,
                             boolean dryRun, URI endpoint) {
        this.account = account;
        this.message = message;
        this.secrets = secrets;
        this.clock = clock;
        this.dryRun = dryRun;
        this.endpoint = endpoint;
        client = HttpClient.newBuilder().connectTimeout(account.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public ProviderResult send(ProviderSendRequest request, CancellationToken token) {
        if (token.cancelled()) return failure("CANCELLED", ErrorCategory.CANCELLED, false,
                "SMS send was cancelled before request");
        if (!request.deadline().isAfter(clock.now())) return failure("ITEM_TIMEOUT",
                ErrorCategory.TIMEOUT, false, "SMS deadline elapsed before request");
        try {
            String phone = StandardProviderSupport.recipientText(request.recipient(), "phoneNumber", true)
                    .replace(" ", "").replace("-", "");
            if (!phone.matches("\\+?[0-9]{6,20}")) {
                throw StandardProviderSupport.invalid("recipient.phoneNumber", "INVALID_PHONE_NUMBER",
                        "phoneNumber must contain 6 to 20 digits with an optional leading plus");
            }
            String templateParams = StandardProviderSupport.renderJson(
                    message.templateParamJsonTemplate(), request.recipient());
            if (!StandardProviderSupport.JSON.readTree(templateParams).isObject()) {
                throw StandardProviderSupport.invalid("templateParamJsonTemplate",
                        "TEMPLATE_PARAMS_OBJECT_REQUIRED", "Rendered template parameters must be a JSON object");
            }
            if (dryRun) return ProviderResult.success("DRY_RUN", "");

            TreeMap<String, String> parameters = parameters(request, phone, templateParams);
            String accessKeySecret = AbstractBotProviderFactory.resolve(secrets, account.accessKeySecret());
            URI uri = signedUri(parameters, accessKeySecret);
            Duration timeout = Duration.between(clock.now(), request.deadline());
            HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(uri).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            return classify(response);
        } catch (ProviderConfigException problem) {
            return failure(problem.code(), problem.path().startsWith("recipient.")
                    ? ErrorCategory.RECIPIENT_INVALID : ErrorCategory.INVALID_REQUEST,
                    false, problem.getMessage());
        } catch (HttpTimeoutException problem) {
            return unknown("ALIYUN_TIMEOUT", ErrorCategory.TIMEOUT,
                    "SMS outcome is unknown after timeout");
        } catch (ConnectException problem) {
            return failure("ALIYUN_CONNECT_FAILED", ErrorCategory.NETWORK, true,
                    "Aliyun endpoint connection failed before a response");
        } catch (IOException problem) {
            return unknown("ALIYUN_IO_UNKNOWN", ErrorCategory.NETWORK,
                    "SMS outcome is unknown after I/O failure");
        } catch (InterruptedException problem) {
            Thread.currentThread().interrupt();
            return unknown("ALIYUN_INTERRUPTED", ErrorCategory.CANCELLED,
                    "SMS outcome is unknown after interruption");
        } catch (GeneralSecurityException problem) {
            return failure("ALIYUN_SIGNATURE_FAILED", ErrorCategory.INTERNAL, false,
                    "Aliyun request signature could not be generated");
        } catch (RuntimeException problem) {
            return failure("ALIYUN_REQUEST_INVALID", ErrorCategory.INVALID_REQUEST, false,
                    problem.getClass().getSimpleName());
        }
    }

    private TreeMap<String, String> parameters(ProviderSendRequest request, String phone,
                                               String templateParams) {
        TreeMap<String, String> values = new TreeMap<>();
        values.put("Action", "SendSms");
        values.put("Version", "2017-05-25");
        values.put("Format", "JSON");
        values.put("RegionId", account.regionId());
        values.put("AccessKeyId", account.accessKeyId());
        values.put("SignatureMethod", "HMAC-SHA1");
        values.put("Timestamp", DateTimeFormatter.ISO_INSTANT.format(clock.now()));
        values.put("SignatureVersion", "1.0");
        values.put("SignatureNonce", UUID.randomUUID().toString());
        values.put("PhoneNumbers", phone);
        values.put("SignName", message.signName());
        values.put("TemplateCode", message.templateCode());
        values.put("TemplateParam", templateParams);
        if (!message.smsUpExtendCode().isBlank()) values.put("SmsUpExtendCode", message.smsUpExtendCode());
        if (!request.idempotencyKey().isBlank()) {
            values.put("OutId", request.idempotencyKey().substring(0,
                    Math.min(request.idempotencyKey().length(), 64)));
        }
        return values;
    }

    private URI signedUri(TreeMap<String, String> parameters, String secret)
            throws GeneralSecurityException {
        StringBuilder canonical = new StringBuilder();
        parameters.forEach((key, value) -> canonical.append('&').append(percentEncode(key))
                .append('=').append(percentEncode(value)));
        String stringToSign = "GET&%2F&" + percentEncode(canonical.substring(1));
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((secret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        String signature = Base64.getEncoder().encodeToString(
                mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        return URI.create(endpoint.toASCIIString() + (endpoint.getRawQuery() == null ? "?" : "&")
                + "Signature=" + percentEncode(signature) + canonical);
    }

    static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private ProviderResult classify(HttpResponse<InputStream> response) throws IOException {
        byte[] bytes;
        try (InputStream input = response.body()) { bytes = input.readNBytes(65_537); }
        if (bytes.length > 65_536) return unknown("ALIYUN_RESPONSE_TOO_LARGE",
                ErrorCategory.UNKNOWN, "SMS outcome is unknown because the response exceeded 64 KiB");
        int status = response.statusCode();
        if (status == 401) return failure("HTTP_401", ErrorCategory.AUTHENTICATION, false,
                "Aliyun credentials were rejected");
        if (status == 403) return failure("HTTP_403", ErrorCategory.AUTHORIZATION, false,
                "Aliyun SMS request was forbidden");
        if (status == 429) return rate("HTTP_429", retryAfter(response));
        if (status >= 500) return unknown("HTTP_" + status, ErrorCategory.TEMPORARY_REMOTE,
                "SMS outcome is unknown after remote server error");
        if (status < 200 || status >= 300) return failure("HTTP_" + status,
                ErrorCategory.PERMANENT_REMOTE, false, "Aliyun rejected the HTTP request");
        JsonNode body;
        try { body = StandardProviderSupport.JSON.readTree(bytes); }
        catch (IOException problem) { return unknown("ALIYUN_RESPONSE_INVALID",
                ErrorCategory.UNKNOWN, "SMS outcome is unknown because the response was not valid JSON"); }
        String rawCode = body.path("Code").asText("");
        String code = "ALIYUN_" + StandardProviderSupport.safeCode(rawCode);
        if (rawCode.equalsIgnoreCase("OK")) {
            String requestId = body.path("RequestId").asText("");
            String bizId = body.path("BizId").asText("");
            return new ProviderResult(ItemState.SUCCEEDED, "ALIYUN_OK", ErrorCategory.NONE,
                    false, null, "", requestId, bizId.isBlank() ? Map.of()
                    : StandardProviderSupport.metadata("bizId", bizId));
        }
        String normalized = rawCode.toUpperCase(Locale.ROOT);
        if (normalized.contains("THROTTL") || normalized.contains("BUSINESS_LIMIT_CONTROL")) {
            return rate(code, Duration.ofMinutes(1));
        }
        if (normalized.contains("ACCESSKEY") || normalized.contains("SIGNATURE")
                || normalized.contains("SECURITYTOKEN") || normalized.contains("ACCOUNT_NOT_EXISTS")) {
            return failure(code, ErrorCategory.AUTHENTICATION, false, "Aliyun credentials were rejected");
        }
        if (normalized.contains("FORBIDDEN") || normalized.contains("PERMISSION")) {
            return failure(code, ErrorCategory.AUTHORIZATION, false, "Aliyun SMS permission was denied");
        }
        if (normalized.contains("MOBILE") || normalized.contains("BLACK_KEY_CONTROL")) {
            return failure(code, ErrorCategory.RECIPIENT_INVALID, false,
                    "Aliyun rejected the recipient phone number");
        }
        if (normalized.contains("TEMPLATE") || normalized.contains("SIGN_NAME")
                || normalized.contains("PARAM")) {
            return failure(code, ErrorCategory.INVALID_REQUEST, false,
                    "Aliyun rejected the SMS template configuration");
        }
        if (normalized.contains("SYSTEM_ERROR") || normalized.contains("SERVICE_UNAVAILABLE")) {
            return failure(code, ErrorCategory.TEMPORARY_REMOTE, true,
                    "Aliyun reported a temporary service error");
        }
        return failure(code, ErrorCategory.PERMANENT_REMOTE, false, "Aliyun rejected the SMS request");
    }

    private static Duration retryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("retry-after").flatMap(value -> {
            try {
                long seconds = Long.parseLong(value);
                return seconds >= 0 && seconds <= 3600 ? java.util.Optional.of(Duration.ofSeconds(seconds))
                        : java.util.Optional.empty();
            } catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
        }).orElse(null);
    }

    private static ProviderResult rate(String code, Duration retryAfter) {
        return new ProviderResult(ItemState.FAILED, code, ErrorCategory.RATE_LIMITED,
                true, retryAfter, "Aliyun SMS rate limit reached", "", Map.of());
    }

    private static ProviderResult failure(String code, ErrorCategory category,
                                          boolean retryable, String diagnostic) {
        return ProviderResult.failure(code, category, retryable, diagnostic);
    }

    private static ProviderResult unknown(String code, ErrorCategory category, String diagnostic) {
        return new ProviderResult(ItemState.UNKNOWN, code, category, false,
                null, diagnostic, "", Map.of());
    }

    @Override
    public void close() { }
}

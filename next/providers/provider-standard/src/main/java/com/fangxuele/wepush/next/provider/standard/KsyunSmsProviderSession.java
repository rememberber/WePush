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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** SendSms protocol from the published com.ksyun:ksc-sdk-sms-java:0.1.8 sources. */
final class KsyunSmsProviderSession implements ProviderSession {
    private final KsyunSmsProviderConfig.Account account;
    private final KsyunSmsProviderConfig.Message message;
    private final SecretResolver secrets;
    private final ExecutionClock clock;
    private final boolean dryRun;
    private final URI endpoint;
    private final HttpClient client;

    KsyunSmsProviderSession(KsyunSmsProviderConfig.Account account,
                           KsyunSmsProviderConfig.Message message, SecretResolver secrets,
                           ExecutionClock clock, boolean dryRun, URI endpoint) {
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
        if (token.cancelled()) return failure("CANCELLED", ErrorCategory.CANCELLED, "SMS send was cancelled");
        if (!request.deadline().isAfter(clock.now())) return failure("ITEM_TIMEOUT", ErrorCategory.TIMEOUT,
                "SMS deadline elapsed before request");
        try {
            String phone = StandardProviderSupport.recipientText(request.recipient(), "phoneNumber", true)
                    .replace(" ", "").replace("-", "");
            if (!phone.matches("\\+?[0-9]{6,20}")) {
                throw StandardProviderSupport.invalid("recipient.phoneNumber", "INVALID_PHONE_NUMBER",
                        "phoneNumber must contain 6 to 20 digits with an optional leading plus");
            }
            String templateParams = StandardProviderSupport.renderJson(
                    message.templateParamJsonTemplate(), request.recipient());
            JsonNode parsed = StandardProviderSupport.JSON.readTree(templateParams);
            if (parsed == null || !parsed.isObject()) {
                throw StandardProviderSupport.invalid("templateParamJsonTemplate", "TEMPLATE_PARAMS_OBJECT_REQUIRED",
                        "Rendered template parameters must be a JSON object");
            }
            if (dryRun) return ProviderResult.success("DRY_RUN", "");
            TreeMap<String, String> parameters = parameters(phone, templateParams);
            String secret = AbstractBotProviderFactory.resolve(secrets, account.accessKeySecret());
            parameters.put("Signature", signature(parameters, secret));
            if (token.cancelled()) return failure("CANCELLED", ErrorCategory.CANCELLED, "SMS send was cancelled");
            Duration timeout = Duration.between(clock.now(), request.deadline());
            if (timeout.isNegative() || timeout.isZero()) return failure("ITEM_TIMEOUT", ErrorCategory.TIMEOUT,
                    "SMS deadline elapsed before request");
            HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(canonical(parameters))).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            return classify(response);
        } catch (ProviderConfigException problem) {
            return failure(problem.code(), problem.path().startsWith("recipient.")
                    ? ErrorCategory.RECIPIENT_INVALID : ErrorCategory.INVALID_REQUEST, problem.getMessage());
        } catch (InterruptedException problem) {
            Thread.currentThread().interrupt();
            return unknown("KSYUN_INTERRUPTED", ErrorCategory.CANCELLED, "SMS outcome is unknown after interruption");
        } catch (HttpTimeoutException problem) {
            return unknown("KSYUN_TIMEOUT", ErrorCategory.TIMEOUT, "SMS outcome is unknown after timeout");
        } catch (ConnectException problem) {
            return ProviderResult.failure("KSYUN_CONNECT_FAILED", ErrorCategory.TEMPORARY_REMOTE, true,
                    "Could not connect to Ksyun SMS endpoint");
        } catch (IOException problem) {
            return unknown("KSYUN_IO_ERROR", ErrorCategory.UNKNOWN, "SMS outcome is unknown after I/O failure");
        } catch (Exception problem) {
            return failure("KSYUN_SEND_FAILED", ErrorCategory.INVALID_REQUEST, problem.getClass().getSimpleName());
        }
    }

    private TreeMap<String, String> parameters(String phone, String templateParams) {
        TreeMap<String, String> values = new TreeMap<>();
        values.put("Accesskey", account.accessKeyId());
        values.put("Service", "ksms");
        values.put("Action", "SendSms");
        values.put("Version", "2019-05-01");
        values.put("Timestamp", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC).format(clock.now()));
        values.put("SignatureVersion", "1.0");
        values.put("SignatureMethod", "HMAC-SHA256");
        values.put("Region", account.region());
        values.put("Mobile", phone);
        values.put("SignName", message.signName());
        values.put("TplId", message.templateId());
        values.put("TplParams", templateParams);
        if (!message.extId().isBlank()) values.put("ExtId", message.extId());
        return values;
    }

    static String signature(Map<String, String> parameters, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical(parameters).getBytes(StandardCharsets.UTF_8)));
    }

    private static String canonical(Map<String, String> parameters) {
        StringBuilder encoded = new StringBuilder();
        new TreeMap<>(parameters).forEach((key, value) -> {
            if (!encoded.isEmpty()) encoded.append('&');
            encoded.append(percentEncode(key)).append('=').append(percentEncode(value));
        });
        return encoded.toString();
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private ProviderResult classify(HttpResponse<InputStream> response) throws IOException {
        byte[] bytes;
        try (InputStream input = response.body()) { bytes = input.readNBytes(65_537); }
        int status = response.statusCode();
        if (status == 401) return failure("HTTP_401", ErrorCategory.AUTHENTICATION, "Ksyun credentials rejected");
        if (status == 403) return failure("HTTP_403", ErrorCategory.AUTHORIZATION, "Ksyun SMS request forbidden");
        if (status == 429) {
            Duration retry = response.headers().firstValue("retry-after").map(value -> {
                try { long seconds = Long.parseLong(value); return seconds >= 0 && seconds <= 3600
                        ? Duration.ofSeconds(seconds) : null; }
                catch (NumberFormatException ignored) { return null; }
            }).orElse(null);
            return new ProviderResult(ItemState.FAILED, "HTTP_429", ErrorCategory.RATE_LIMITED,
                    true, retry, "Ksyun SMS rate limit reached", "", Map.of());
        }
        if (status >= 500) return unknown("HTTP_" + status, ErrorCategory.TEMPORARY_REMOTE,
                "SMS outcome is unknown after remote server error");
        if (bytes.length > 65_536) return unknown("KSYUN_RESPONSE_TOO_LARGE", ErrorCategory.UNKNOWN,
                "SMS response exceeded 64 KiB");
        JsonNode body;
        try { body = StandardProviderSupport.JSON.readTree(bytes); }
        catch (IOException problem) { return unknown("KSYUN_RESPONSE_INVALID", ErrorCategory.UNKNOWN,
                "SMS response was not valid JSON"); }
        if (body == null || !body.isObject()) return unknown("KSYUN_RESPONSE_INVALID", ErrorCategory.UNKNOWN,
                "SMS response was not a JSON object");
        JsonNode error = body.path("Error");
        if (!error.isMissingNode() && !error.isNull()) {
            String raw = error.path("Code").asText("");
            String code = "KSYUN_" + StandardProviderSupport.safeCode(raw);
            String normalized = raw.toUpperCase(Locale.ROOT);
            ErrorCategory category = ErrorCategory.PERMANENT_REMOTE;
            if (normalized.contains("SIGNATURE") || normalized.contains("ACCESSKEY") || normalized.contains("AUTH"))
                category = ErrorCategory.AUTHENTICATION;
            else if (normalized.contains("FORBIDDEN") || normalized.contains("PERMISSION")) category = ErrorCategory.AUTHORIZATION;
            else if (normalized.contains("MOBILE") || normalized.contains("PHONE")) category = ErrorCategory.RECIPIENT_INVALID;
            else if (normalized.contains("TPL") || normalized.contains("TEMPLATE") || normalized.contains("PARAM")
                    || normalized.contains("SIGNNAME")) category = ErrorCategory.INVALID_REQUEST;
            else if (normalized.contains("THROTTL") || normalized.contains("LIMIT")) category = ErrorCategory.RATE_LIMITED;
            return ProviderResult.failure(code, category, category == ErrorCategory.RATE_LIMITED,
                    "Ksyun rejected the SMS request");
        }
        if (status < 200 || status >= 300) return failure("HTTP_" + status, ErrorCategory.PERMANENT_REMOTE,
                "Ksyun rejected the HTTP request");
        String sid = body.path("Sid").asText("");
        if (sid.isBlank()) return unknown("KSYUN_RESPONSE_INVALID", ErrorCategory.UNKNOWN,
                "SMS response did not contain Sid");
        return new ProviderResult(ItemState.SUCCEEDED, "KSYUN_OK", ErrorCategory.NONE, false, null, "",
                body.path("RequestId").asText(""), StandardProviderSupport.metadata("sid", sid));
    }

    private static ProviderResult failure(String code, ErrorCategory category, String diagnostic) {
        return ProviderResult.failure(code, category, false, diagnostic);
    }

    private static ProviderResult unknown(String code, ErrorCategory category, String diagnostic) {
        return new ProviderResult(ItemState.UNKNOWN, code, category, false, null, diagnostic, "", Map.of());
    }

    @Override
    public void close() { client.close(); }
}

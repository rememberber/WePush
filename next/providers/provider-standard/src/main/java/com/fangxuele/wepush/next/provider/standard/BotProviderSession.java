package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

final class BotProviderSession implements ProviderSession {
    private final BotVendor vendor;
    private final BotProviderConfig.Account account;
    private final BotProviderConfig.Message message;
    private final SecretResolver secrets;
    private final ExecutionClock clock;
    private final boolean dryRun;
    private final boolean allowTestEndpoints;
    private final HttpClient client;
    private final SlidingWindowGate limiter;

    BotProviderSession(BotVendor vendor, BotProviderConfig.Account account,
                       BotProviderConfig.Message message, SecretResolver secrets,
                       ExecutionClock clock, boolean dryRun, boolean allowTestEndpoints) {
        this.vendor = vendor;
        this.account = account;
        this.message = message;
        this.secrets = secrets;
        this.clock = clock;
        this.dryRun = dryRun;
        this.allowTestEndpoints = allowTestEndpoints;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        limiter = new SlidingWindowGate(vendor.firstLimit, vendor.firstWindow,
                vendor.secondLimit, vendor.secondWindow);
    }

    @Override
    public ProviderResult send(ProviderSendRequest request, CancellationToken token) {
        if (token.cancelled()) return failure("CANCELLED", ErrorCategory.CANCELLED, false,
                "Bot send was cancelled before request");
        if (!request.deadline().isAfter(clock.now())) return failure("ITEM_TIMEOUT",
                ErrorCategory.TIMEOUT, false, "Bot deadline elapsed before request");
        try {
            ObjectNode payload = payload(request);
            byte[] body = StandardProviderSupport.JSON.writeValueAsBytes(payload);
            if (body.length > vendor.maximumPayloadBytes) return failure("PAYLOAD_TOO_LARGE",
                    ErrorCategory.INVALID_REQUEST, false, "Bot payload exceeds provider size limit");
            if (dryRun) return ProviderResult.success("DRY_RUN", "");

            Duration retryAfter = limiter.acquire(clock.now());
            if (!retryAfter.isZero()) return new ProviderResult(ItemState.FAILED,
                    "LOCAL_RATE_LIMIT", ErrorCategory.RATE_LIMITED, true, retryAfter,
                    "Provider safety rate limit reached", "", Map.of());

            URI webhook = AbstractBotProviderFactory.resolveUri(secrets, account.webhook());
            vendor.validateEndpoint(webhook, allowTestEndpoints);
            if (vendor == BotVendor.DINGTALK && account.signingSecret() != null) {
                webhook = signedDingTalkUri(webhook, request);
            } else if (vendor == BotVendor.FEISHU && account.signingSecret() != null) {
                addFeishuSignature(payload, request);
                body = StandardProviderSupport.JSON.writeValueAsBytes(payload);
            }
            Duration timeout = Duration.between(clock.now(), request.deadline());
            HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(webhook)
                            .timeout(timeout).header("Content-Type", "application/json; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            return classify(response);
        } catch (ProviderConfigException problem) {
            return failure(problem.code(), problem.path().startsWith("recipient.")
                    ? ErrorCategory.RECIPIENT_INVALID : ErrorCategory.INVALID_REQUEST,
                    false, problem.getMessage());
        } catch (HttpTimeoutException problem) {
            return unknown("BOT_TIMEOUT", ErrorCategory.TIMEOUT,
                    "Bot outcome is unknown after timeout");
        } catch (ConnectException problem) {
            return failure("BOT_CONNECT_FAILED", ErrorCategory.NETWORK, true,
                    "Bot endpoint connection failed before a response");
        } catch (IOException problem) {
            return unknown("BOT_IO_UNKNOWN", ErrorCategory.NETWORK,
                    "Bot outcome is unknown after I/O failure");
        } catch (InterruptedException problem) {
            Thread.currentThread().interrupt();
            return unknown("BOT_INTERRUPTED", ErrorCategory.CANCELLED,
                    "Bot outcome is unknown after interruption");
        } catch (GeneralSecurityException problem) {
            return failure("BOT_SIGNATURE_FAILED", ErrorCategory.INTERNAL, false,
                    "Bot request signature could not be generated");
        } catch (RuntimeException problem) {
            return failure("BOT_REQUEST_INVALID", ErrorCategory.INVALID_REQUEST, false,
                    problem.getClass().getSimpleName());
        }
    }

    private ObjectNode payload(ProviderSendRequest request) {
        if (message.type().equals("RAW")) return renderedObject(message.rawJsonTemplate(), request);
        if (vendor == BotVendor.FEISHU && message.type().equals("INTERACTIVE")) {
            ObjectNode root = StandardProviderSupport.JSON.createObjectNode();
            root.put("msg_type", "interactive");
            root.set("card", renderedObject(message.rawJsonTemplate(), request));
            return root;
        }
        return switch (vendor) {
            case FEISHU -> feishu(request);
            case DINGTALK -> dingTalk(request);
            case WECOM -> weCom(request);
        };
    }

    private ObjectNode feishu(ProviderSendRequest request) {
        String content = keyword(StandardProviderSupport.renderText(message.contentTemplate(), request.recipient()));
        String mention = mention(request);
        ObjectNode root = StandardProviderSupport.JSON.createObjectNode();
        if (message.type().equals("TEXT")) {
            root.put("msg_type", "text");
            root.putObject("content").put("text", content + feishuMention(mention));
        } else {
            root.put("msg_type", "post");
            ObjectNode locale = root.putObject("content").putObject("post").putObject("zh_cn");
            locale.put("title", StandardProviderSupport.renderText(message.titleTemplate(), request.recipient()));
            ArrayNode paragraph = locale.putArray("content").addArray();
            paragraph.addObject().put("tag", "text").put("text", content);
            if (!mention.isBlank()) paragraph.addObject().put("tag", "at").put("user_id", mention);
        }
        return root;
    }

    private ObjectNode dingTalk(ProviderSendRequest request) {
        String content = StandardProviderSupport.renderText(message.contentTemplate(), request.recipient());
        String mention = mention(request);
        ObjectNode root = StandardProviderSupport.JSON.createObjectNode();
        switch (message.type()) {
            case "TEXT" -> {
                root.put("msgtype", "text").putObject("text").put("content", content);
                dingAt(root, mention);
            }
            case "MARKDOWN" -> {
                root.put("msgtype", "markdown");
                root.putObject("markdown")
                        .put("title", StandardProviderSupport.renderText(message.titleTemplate(), request.recipient()))
                        .put("text", content);
                dingAt(root, mention);
            }
            case "LINK" -> {
                root.put("msgtype", "link");
                root.putObject("link")
                        .put("title", StandardProviderSupport.renderText(message.titleTemplate(), request.recipient()))
                        .put("text", content)
                        .put("messageUrl", StandardProviderSupport.renderText(message.urlTemplate(), request.recipient()))
                        .put("picUrl", StandardProviderSupport.renderText(message.pictureUrlTemplate(), request.recipient()));
            }
            case "ACTION_CARD" -> {
                root.put("msgtype", "actionCard");
                root.putObject("actionCard")
                        .put("title", StandardProviderSupport.renderText(message.titleTemplate(), request.recipient()))
                        .put("text", content)
                        .put("singleTitle", StandardProviderSupport.renderText(message.buttonTextTemplate(), request.recipient()))
                        .put("singleURL", StandardProviderSupport.renderText(message.buttonUrlTemplate(), request.recipient()));
            }
            default -> throw StandardProviderSupport.invalid("type", "MESSAGE_TYPE_UNSUPPORTED", "Unsupported DingTalk type");
        }
        return root;
    }

    private ObjectNode weCom(ProviderSendRequest request) {
        String content = StandardProviderSupport.renderText(message.contentTemplate(), request.recipient());
        ObjectNode root = StandardProviderSupport.JSON.createObjectNode();
        if (message.type().equals("TEXT")) {
            root.put("msgtype", "text");
            ObjectNode text = root.putObject("text").put("content", content);
            String mention = mention(request);
            if (!mention.isBlank()) text.putArray("mentioned_mobile_list").add(mention);
        } else {
            root.put("msgtype", "markdown").putObject("markdown").put("content", content);
        }
        return root;
    }

    private void dingAt(ObjectNode root, String mention) {
        ObjectNode at = root.putObject("at").put("isAtAll", message.mentionAll());
        if (!mention.isBlank() && !message.mentionAll()) at.putArray("atMobiles").add(mention);
    }

    private String mention(ProviderSendRequest request) {
        if (message.mentionAll()) return vendor == BotVendor.FEISHU ? "all" : "@all";
        return message.mentionField().isBlank() ? ""
                : StandardProviderSupport.recipientText(request.recipient(), message.mentionField(), false);
    }

    private static String feishuMention(String mention) {
        return mention.isBlank() ? "" : " <at user_id=\"" + mention + "\">mention</at>";
    }

    private String keyword(String content) {
        return account.keyword().isBlank() ? content : account.keyword() + " " + content;
    }

    private ObjectNode renderedObject(String template, ProviderSendRequest request) {
        try {
            JsonNode value = StandardProviderSupport.JSON.readTree(
                    StandardProviderSupport.renderJson(template, request.recipient()));
            if (!value.isObject()) throw StandardProviderSupport.invalid("rawJsonTemplate",
                    "RAW_JSON_OBJECT_REQUIRED", "Rendered raw JSON must be an object");
            return (ObjectNode) value;
        } catch (IOException problem) {
            throw StandardProviderSupport.invalid("rawJsonTemplate", "RENDERED_JSON_INVALID",
                    "Rendered raw JSON is invalid");
        }
    }

    private void addFeishuSignature(ObjectNode payload, ProviderSendRequest request)
            throws GeneralSecurityException {
        long timestamp = clock.now().getEpochSecond();
        String secret = AbstractBotProviderFactory.resolve(secrets, account.signingSecret());
        payload.put("timestamp", Long.toString(timestamp));
        payload.put("sign", hmac(timestamp + "\n" + secret, "", secret, false));
    }

    private URI signedDingTalkUri(URI webhook, ProviderSendRequest request)
            throws GeneralSecurityException {
        long timestamp = clock.now().toEpochMilli();
        String secret = AbstractBotProviderFactory.resolve(secrets, account.signingSecret());
        String signature = hmac(timestamp + "\n" + secret, timestamp + "\n" + secret, secret, true);
        String separator = webhook.getRawQuery() == null ? "?" : "&";
        return URI.create(webhook.toASCIIString() + separator + "timestamp=" + timestamp
                + "&sign=" + URLEncoder.encode(signature, StandardCharsets.UTF_8));
    }

    private static String hmac(String keyMaterial, String data, String secret, boolean dingTalk)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec((dingTalk ? secret : keyMaterial).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] payload = data.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(mac.doFinal(payload));
    }

    private ProviderResult classify(HttpResponse<InputStream> response) throws IOException {
        byte[] bytes;
        try (InputStream input = response.body()) { bytes = input.readNBytes(65_537); }
        if (bytes.length > 65_536) return unknown("BOT_RESPONSE_TOO_LARGE",
                ErrorCategory.UNKNOWN, "Bot outcome is unknown because the response exceeded 64 KiB");
        int status = response.statusCode();
        if (status == 401) return failure("HTTP_401", ErrorCategory.AUTHENTICATION, false, "Bot authentication failed");
        if (status == 403) return failure("HTTP_403", ErrorCategory.AUTHORIZATION, false, "Bot request was forbidden");
        if (status == 429) return rate("HTTP_429", retryAfter(response));
        if (status >= 500) return unknown("HTTP_" + status, ErrorCategory.TEMPORARY_REMOTE,
                "Bot outcome is unknown after remote server error");
        if (status < 200 || status >= 300) return failure("HTTP_" + status,
                ErrorCategory.PERMANENT_REMOTE, false, "Bot endpoint rejected the HTTP request");
        JsonNode body;
        try { body = StandardProviderSupport.JSON.readTree(bytes); }
        catch (IOException problem) { return unknown("BOT_RESPONSE_INVALID", ErrorCategory.UNKNOWN,
                "Bot outcome is unknown because the response was not valid JSON"); }
        String rawCode = vendor == BotVendor.FEISHU
                ? body.path("code").isMissingNode() ? body.path("StatusCode").asText("") : body.path("code").asText("")
                : body.path("errcode").asText("");
        if (rawCode.equals("0")) {
            return new ProviderResult(ItemState.SUCCEEDED, vendor.name() + "_OK", ErrorCategory.NONE,
                    false, null, "", requestId(response), StandardProviderSupport.metadata("httpStatus", status));
        }
        String code = vendor.name() + "_" + StandardProviderSupport.safeCode(rawCode);
        String messageText = body.path(vendor == BotVendor.FEISHU ? "msg" : "errmsg").asText("")
                .toLowerCase(Locale.ROOT);
        if (messageText.contains("frequency") || messageText.contains("rate")
                || messageText.contains("频率") || messageText.contains("limit")) return rate(code, null);
        if (messageText.contains("sign") || messageText.contains("token") || messageText.contains("key")
                || messageText.contains("签名") || messageText.contains("密钥")) {
            return failure(code, ErrorCategory.AUTHENTICATION, false, "Bot credential was rejected");
        }
        return failure(code, ErrorCategory.PERMANENT_REMOTE, false, "Bot provider rejected the message");
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

    private static String requestId(HttpResponse<?> response) {
        return response.headers().firstValue("x-request-id")
                .or(() -> response.headers().firstValue("x-tt-logid"))
                .orElse("");
    }

    private static ProviderResult rate(String code, Duration retryAfter) {
        return new ProviderResult(ItemState.FAILED, code, ErrorCategory.RATE_LIMITED, true,
                retryAfter, "Bot provider rate limit reached", "", Map.of());
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

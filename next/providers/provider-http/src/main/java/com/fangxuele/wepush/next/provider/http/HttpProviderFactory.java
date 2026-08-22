package com.fangxuele.wepush.next.provider.http;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

public final class HttpProviderFactory implements ProviderFactory {
    public static final String PROVIDER_ID = "wepush.http";
    public static final String VERSION = "0.1.0";

    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            PROVIDER_ID,
            "HTTP",
            VERSION,
            1,
            Set.of(
                    ProviderDescriptor.Capability.DRY_RUN,
                    ProviderDescriptor.Capability.IDEMPOTENCY,
                    ProviderDescriptor.Capability.RESPONSE_BODY),
            ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE,
            256,
            Duration.ofSeconds(30),
            schema("account.schema.json"),
            schema("message.schema.json"),
            schema("recipient.schema.json"));

    @Override
    public ProviderDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ValidationResult validateAccount(ConfigDocument account) {
        try {
            HttpProviderConfig.parseAccount(account);
            return ValidationResult.valid();
        } catch (HttpProviderConfig.ConfigException exception) {
            return ValidationResult.invalid(exception.path(), exception.code(), exception.getMessage());
        }
    }

    @Override
    public ValidationResult validateMessage(ConfigDocument message) {
        try {
            HttpProviderConfig.parseMessage(message);
            return ValidationResult.valid();
        } catch (HttpProviderConfig.ConfigException exception) {
            return ValidationResult.invalid(exception.path(), exception.code(), exception.getMessage());
        }
    }

    @Override
    public ConnectionTestResult testConnection(
            ConfigDocument accountDocument,
            SecretResolver secrets,
            Duration timeout
    ) {
        Instant started = Instant.now();
        try {
            HttpProviderConfig.Account account = HttpProviderConfig.parseAccount(accountDocument);
            SsrfGuard.verify(account.baseUrl(), account.allowPrivateAddresses());
            HttpRequest.Builder request = HttpRequest.newBuilder(account.baseUrl())
                    .timeout(timeout)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody());
            account.defaultHeaders().forEach(request::header);
            applyAuthentication(request, account, secrets);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(account.connectTimeout())
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            int status = client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
            boolean reachable = status < 500;
            return new ConnectionTestResult(
                    reachable,
                    "HTTP_" + status,
                    reachable ? "" : "Remote endpoint returned a server error",
                    Duration.between(started, Instant.now()));
        } catch (Exception exception) {
            return new ConnectionTestResult(
                    false,
                    "CONNECTION_TEST_FAILED",
                    exception.getClass().getSimpleName(),
                    Duration.between(started, Instant.now()));
        }
    }

    @Override
    public ProviderSession open(ProviderOpenContext context) {
        HttpProviderConfig.Account account = HttpProviderConfig.parseAccount(context.spec().accountConfig());
        HttpProviderConfig.Message message = HttpProviderConfig.parseMessage(context.spec().messageConfig());
        return new HttpProviderSession(
                account, message, context.secretResolver(), context.spec().dryRun());
    }

    private static void applyAuthentication(
            HttpRequest.Builder request,
            HttpProviderConfig.Account account,
            SecretResolver secrets
    ) {
        if (account.auth().type() == HttpProviderConfig.AuthType.NONE) {
            return;
        }
        try (SecretValue secret = secrets.resolve(account.auth().secretRef())) {
            if (secret == null) {
                throw new IllegalArgumentException("SecretResolver returned null");
            }
            char[] value = secret.copyChars();
            try {
                request.header("Authorization", "Bearer " + new String(value));
            } finally {
                Arrays.fill(value, '\0');
            }
        }
    }

    private static ConfigDocument schema(String name) {
        String path = "/META-INF/wepush/schemas/" + name;
        try (InputStream input = HttpProviderFactory.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new ExceptionInInitializerError("Missing HTTP provider schema: " + path);
            }
            return new ConfigDocument(
                    PROVIDER_ID + "/" + name,
                    "1",
                    ConfigDocument.JSON_MEDIA_TYPE,
                    input.readAllBytes());
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}

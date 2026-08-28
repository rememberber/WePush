package com.fangxuele.wepush.next.provider.standard;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class AliyunSmsProviderFactory implements ProviderFactory {
    public static final String PROVIDER_ID = "wepush.sms.aliyun";
    public static final String VERSION = "0.1.0";
    private static final URI PRODUCTION_ENDPOINT = URI.create("https://dysmsapi.aliyuncs.com/");
    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            PROVIDER_ID, "Aliyun SMS", VERSION, 1,
            Set.of(ProviderDescriptor.Capability.DRY_RUN),
            ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE, 10, Duration.ofSeconds(30),
            StandardProviderSupport.schema(AliyunSmsProviderFactory.class, PROVIDER_ID,
                    "aliyun-sms", "account"),
            StandardProviderSupport.schema(AliyunSmsProviderFactory.class, PROVIDER_ID,
                    "aliyun-sms", "message"),
            StandardProviderSupport.schema(AliyunSmsProviderFactory.class, PROVIDER_ID,
                    "aliyun-sms", "recipient"));

    private final URI endpoint;

    public AliyunSmsProviderFactory() { this(PRODUCTION_ENDPOINT); }

    AliyunSmsProviderFactory(URI endpoint) {
        if (!isProduction(endpoint) && !isLoopback(endpoint)) {
            throw new IllegalArgumentException("Aliyun SMS endpoint must be the official endpoint");
        }
        this.endpoint = endpoint;
    }

    @Override
    public ProviderDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ValidationResult validateAccount(ConfigDocument account) {
        return StandardProviderSupport.validation(() -> AliyunSmsProviderConfig.account(account));
    }

    @Override
    public ValidationResult validateMessage(ConfigDocument message) {
        return StandardProviderSupport.validation(() -> AliyunSmsProviderConfig.message(message));
    }

    @Override
    public ConnectionTestResult testConnection(ConfigDocument accountDocument, SecretResolver secrets,
                                               Duration timeout) {
        Instant started = Instant.now();
        try {
            AliyunSmsProviderConfig.Account account = AliyunSmsProviderConfig.account(accountDocument);
            AbstractBotProviderFactory.resolve(secrets, account.accessKeySecret());
            HttpResponse<Void> response = HttpClient.newBuilder().connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NEVER).build().send(
                            HttpRequest.newBuilder(endpoint).timeout(timeout)
                                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                            HttpResponse.BodyHandlers.discarding());
            boolean reachable = response.statusCode() < 500;
            return new ConnectionTestResult(reachable, reachable ? "ALIYUN_ENDPOINT_REACHABLE"
                    : "ALIYUN_ENDPOINT_UNAVAILABLE", reachable ? "Credential presence and endpoint reachability verified"
                    : "Aliyun SMS endpoint returned a server error", Duration.between(started, Instant.now()));
        } catch (Exception problem) {
            return new ConnectionTestResult(false, "ALIYUN_CONNECTION_TEST_FAILED",
                    problem.getClass().getSimpleName(), Duration.between(started, Instant.now()));
        }
    }

    @Override
    public ProviderSession open(ProviderOpenContext context) {
        return new AliyunSmsProviderSession(
                AliyunSmsProviderConfig.account(context.spec().accountConfig()),
                AliyunSmsProviderConfig.message(context.spec().messageConfig()),
                context.secretResolver(), context.clock(), context.spec().dryRun(), endpoint);
    }

    private static boolean isProduction(URI endpoint) {
        return endpoint != null && endpoint.getScheme().equals("https")
                && endpoint.getHost().equalsIgnoreCase("dysmsapi.aliyuncs.com")
                && (endpoint.getPort() == -1 || endpoint.getPort() == 443)
                && (endpoint.getPath().isEmpty() || endpoint.getPath().equals("/"));
    }

    private static boolean isLoopback(URI endpoint) {
        if (endpoint == null || endpoint.getHost() == null) return false;
        return endpoint.getScheme().equals("http")
                && (endpoint.getHost().equals("127.0.0.1")
                || endpoint.getHost().equals("localhost")
                || endpoint.getHost().equals("::1"));
    }
}

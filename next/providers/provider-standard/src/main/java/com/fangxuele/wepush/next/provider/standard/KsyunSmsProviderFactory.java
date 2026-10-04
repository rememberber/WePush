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

public final class KsyunSmsProviderFactory implements ProviderFactory {
    public static final String PROVIDER_ID = "wepush.sms.ksyun";
    public static final String VERSION = "0.1.0";
    private static final URI PRODUCTION_ENDPOINT = URI.create("https://ksms.ksyun.com/");
    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            PROVIDER_ID, "Ksyun SMS", VERSION, 1,
            Set.of(ProviderDescriptor.Capability.DRY_RUN),
            ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE, 10, Duration.ofSeconds(30),
            StandardProviderSupport.schema(KsyunSmsProviderFactory.class, PROVIDER_ID,
                    "ksyun-sms", "account"),
            StandardProviderSupport.schema(KsyunSmsProviderFactory.class, PROVIDER_ID,
                    "ksyun-sms", "message"),
            StandardProviderSupport.schema(KsyunSmsProviderFactory.class, PROVIDER_ID,
                    "ksyun-sms", "recipient"));

    private final URI endpoint;

    public KsyunSmsProviderFactory() { this.endpoint = null; }

    KsyunSmsProviderFactory(URI endpoint) {
        if (!isProduction(endpoint) && !isLoopback(endpoint)) {
            throw new IllegalArgumentException("Ksyun SMS endpoint must be the official endpoint");
        }
        this.endpoint = endpoint;
    }

    @Override
    public ProviderDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ValidationResult validateAccount(ConfigDocument account) {
        return StandardProviderSupport.validation(() -> KsyunSmsProviderConfig.account(account));
    }

    @Override
    public ValidationResult validateMessage(ConfigDocument message) {
        return StandardProviderSupport.validation(() -> KsyunSmsProviderConfig.message(message));
    }

    @Override
    public ConnectionTestResult testConnection(ConfigDocument accountDocument, SecretResolver secrets,
                                               Duration timeout) {
        Instant started = Instant.now();
        try {
            KsyunSmsProviderConfig.Account account = KsyunSmsProviderConfig.account(accountDocument);
            AbstractBotProviderFactory.resolve(secrets, account.accessKeySecret());
            HttpResponse<Void> response = HttpClient.newBuilder().connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NEVER).build().send(
                            HttpRequest.newBuilder(endpointFor(account)).timeout(timeout)
                                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                            HttpResponse.BodyHandlers.discarding());
            boolean reachable = response.statusCode() < 500;
            return new ConnectionTestResult(reachable, reachable ? "KSYUN_ENDPOINT_REACHABLE"
                    : "KSYUN_ENDPOINT_UNAVAILABLE", reachable ? "Credential presence and endpoint reachability verified"
                    : "Ksyun SMS endpoint returned a server error", Duration.between(started, Instant.now()));
        } catch (Exception problem) {
            return new ConnectionTestResult(false, "KSYUN_CONNECTION_TEST_FAILED",
                    problem.getClass().getSimpleName(), Duration.between(started, Instant.now()));
        }
    }

    @Override
    public ProviderSession open(ProviderOpenContext context) {
        return new KsyunSmsProviderSession(
                KsyunSmsProviderConfig.account(context.spec().accountConfig()),
                KsyunSmsProviderConfig.message(context.spec().messageConfig()),
                context.secretResolver(), context.clock(), context.spec().dryRun(),
                endpointFor(KsyunSmsProviderConfig.account(context.spec().accountConfig())));
    }

    private URI endpointFor(KsyunSmsProviderConfig.Account account) {
        if (endpoint != null) return endpoint;
        return account.region().equals("cn-guangzhou-1")
                ? URI.create("https://ksms.cn-guangzhou-1.api.ksyun.com/") : PRODUCTION_ENDPOINT;
    }

    private static boolean isProduction(URI endpoint) {
        return endpoint != null && "https".equals(endpoint.getScheme())
                && ("ksms.ksyun.com".equalsIgnoreCase(endpoint.getHost())
                || "ksms.cn-guangzhou-1.api.ksyun.com".equalsIgnoreCase(endpoint.getHost()))
                && (endpoint.getPort() == -1 || endpoint.getPort() == 443)
                && (endpoint.getPath().isEmpty() || endpoint.getPath().equals("/"))
                && endpoint.getUserInfo() == null && endpoint.getRawQuery() == null && endpoint.getFragment() == null;
    }

    private static boolean isLoopback(URI endpoint) {
        if (endpoint == null || endpoint.getHost() == null) return false;
        return endpoint.getScheme().equals("http")
                && (endpoint.getHost().equals("127.0.0.1")
                || endpoint.getHost().equals("localhost")
                || endpoint.getHost().equals("::1"));
    }
}

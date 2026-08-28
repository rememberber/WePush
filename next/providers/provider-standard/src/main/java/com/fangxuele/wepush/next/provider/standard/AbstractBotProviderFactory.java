package com.fangxuele.wepush.next.provider.standard;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.core.api.SecretValue;
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
import java.util.Arrays;
import java.util.Set;

abstract class AbstractBotProviderFactory implements ProviderFactory {
    static final String VERSION = "0.1.0";

    private final BotVendor vendor;
    private final boolean allowTestEndpoints;
    private final ProviderDescriptor descriptor;

    AbstractBotProviderFactory(BotVendor vendor) { this(vendor, false); }

    AbstractBotProviderFactory(BotVendor vendor, boolean allowTestEndpoints) {
        this.vendor = vendor;
        this.allowTestEndpoints = allowTestEndpoints;
        descriptor = new ProviderDescriptor(vendor.providerId, vendor.displayName, VERSION, 1,
                Set.of(ProviderDescriptor.Capability.DRY_RUN),
                ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE, vendor.maximumConcurrency,
                Duration.ofSeconds(30),
                StandardProviderSupport.schema(getClass(), vendor.providerId, vendor.slug, "account"),
                StandardProviderSupport.schema(getClass(), vendor.providerId, vendor.slug, "message"),
                StandardProviderSupport.schema(getClass(), vendor.providerId, vendor.slug, "recipient"));
    }

    @Override
    public final ProviderDescriptor descriptor() { return descriptor; }

    @Override
    public final ValidationResult validateAccount(ConfigDocument account) {
        return StandardProviderSupport.validation(() -> BotProviderConfig.account(vendor, account));
    }

    @Override
    public final ValidationResult validateMessage(ConfigDocument message) {
        return StandardProviderSupport.validation(() -> BotProviderConfig.message(vendor, message));
    }

    @Override
    public final ConnectionTestResult testConnection(ConfigDocument accountDocument,
                                                     SecretResolver secrets, Duration timeout) {
        Instant started = Instant.now();
        try {
            BotProviderConfig.Account account = BotProviderConfig.account(vendor, accountDocument);
            URI webhook = resolveUri(secrets, account.webhook());
            vendor.validateEndpoint(webhook, allowTestEndpoints);
            HttpResponse<Void> response = HttpClient.newBuilder().connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NEVER).build().send(
                            HttpRequest.newBuilder(webhook).timeout(timeout)
                                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                            HttpResponse.BodyHandlers.discarding());
            boolean reachable = response.statusCode() < 500;
            return new ConnectionTestResult(reachable, "HTTP_" + response.statusCode(),
                    reachable ? "" : "Bot endpoint returned a server error",
                    Duration.between(started, Instant.now()));
        } catch (Exception problem) {
            return new ConnectionTestResult(false, "BOT_CONNECTION_TEST_FAILED",
                    problem.getClass().getSimpleName(), Duration.between(started, Instant.now()));
        }
    }

    @Override
    public final ProviderSession open(ProviderOpenContext context) {
        return new BotProviderSession(vendor,
                BotProviderConfig.account(vendor, context.spec().accountConfig()),
                BotProviderConfig.message(vendor, context.spec().messageConfig()),
                context.secretResolver(), context.clock(), context.spec().dryRun(), allowTestEndpoints);
    }

    static URI resolveUri(SecretResolver secrets, com.fangxuele.wepush.next.core.api.SecretRef ref) {
        return URI.create(resolve(secrets, ref));
    }

    static String resolve(SecretResolver secrets, com.fangxuele.wepush.next.core.api.SecretRef ref) {
        try (SecretValue secret = secrets.resolve(ref)) {
            if (secret == null) throw new IllegalArgumentException("SecretResolver returned null");
            char[] value = secret.copyChars();
            try {
                String text = new String(value).trim();
                if (text.isEmpty()) throw new IllegalArgumentException("Secret is empty");
                return text;
            } finally {
                Arrays.fill(value, '\0');
            }
        }
    }
}

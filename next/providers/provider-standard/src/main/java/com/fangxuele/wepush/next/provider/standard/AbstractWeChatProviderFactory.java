package com.fangxuele.wepush.next.provider.standard;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

abstract class AbstractWeChatProviderFactory implements ProviderFactory {
    static final String VERSION = "0.1.0";

    private final WeChatPlatform platform;
    private final URI apiBase;
    private final ProviderDescriptor descriptor;

    AbstractWeChatProviderFactory(WeChatPlatform platform) { this(platform, platform.productionBase()); }

    AbstractWeChatProviderFactory(WeChatPlatform platform, URI apiBase) {
        this.platform = platform;
        platform.validateBase(apiBase);
        this.apiBase = apiBase;
        descriptor = new ProviderDescriptor(platform.providerId, platform.displayName, VERSION, 1,
                Set.of(ProviderDescriptor.Capability.DRY_RUN),
                ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE, platform.maximumConcurrency,
                Duration.ofSeconds(30),
                StandardProviderSupport.schema(getClass(), platform.providerId, platform.slug, "account"),
                StandardProviderSupport.schema(getClass(), platform.providerId, platform.slug, "message"),
                StandardProviderSupport.schema(getClass(), platform.providerId, platform.slug, "recipient"));
    }

    @Override
    public final ProviderDescriptor descriptor() { return descriptor; }

    @Override
    public final ValidationResult validateAccount(ConfigDocument account) {
        return StandardProviderSupport.validation(() -> WeChatProviderConfig.account(platform, account));
    }

    @Override
    public final ValidationResult validateMessage(ConfigDocument message) {
        return StandardProviderSupport.validation(() -> WeChatProviderConfig.message(platform, message));
    }

    @Override
    public final ConnectionTestResult testConnection(ConfigDocument accountDocument,
                                                     SecretResolver secrets, Duration timeout) {
        Instant started = Instant.now();
        try {
            WeChatProviderConfig.Account account = WeChatProviderConfig.account(platform, accountDocument);
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            WeChatProviderSession.fetchAccessToken(platform, account, secrets, ExecutionClock.system(),
                    client, apiBase, timeout);
            return new ConnectionTestResult(true, "ACCESS_TOKEN_VERIFIED", "",
                    Duration.between(started, Instant.now()));
        } catch (WeChatProviderSession.RemoteFailure problem) {
            return new ConnectionTestResult(false, problem.code(), problem.diagnostic(),
                    Duration.between(started, Instant.now()));
        } catch (Exception problem) {
            return new ConnectionTestResult(false, "WECHAT_CONNECTION_TEST_FAILED",
                    problem.getClass().getSimpleName(), Duration.between(started, Instant.now()));
        }
    }

    @Override
    public final ProviderSession open(ProviderOpenContext context) {
        return new WeChatProviderSession(platform,
                WeChatProviderConfig.account(platform, context.spec().accountConfig()),
                WeChatProviderConfig.message(platform, context.spec().messageConfig()),
                context.secretResolver(), context.clock(), context.spec().dryRun(), apiBase);
    }
}

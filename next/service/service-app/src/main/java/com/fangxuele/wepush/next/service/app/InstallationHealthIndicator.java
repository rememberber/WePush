package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.provider.http.HttpProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.service.application.ProviderRegistry;
import org.flywaydb.core.Flyway;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/** Upgrade/restore gate: proves the migrated database and a local-only Provider Dry Run. */
@Component("wePushInstallation")
final class InstallationHealthIndicator implements HealthIndicator {
    private final Flyway flyway;
    private final ProviderRegistry providers;

    InstallationHealthIndicator(Flyway flyway, ProviderRegistry providers) {
        this.flyway = flyway;
        this.providers = providers;
    }

    @Override
    public Health health() {
        try {
            var current = flyway.info().current();
            if (current == null || !current.getState().isApplied()) {
                return Health.down().withDetail("code", "DATABASE_MIGRATION_NOT_APPLIED").build();
            }
            ProviderFactory http = providers.find(HttpProviderFactory.PROVIDER_ID, HttpProviderFactory.VERSION)
                    .orElseThrow(() -> new IllegalStateException("Built-in HTTP Provider is missing"));
            ProviderResult dryRun = dryRun(http);
            if (dryRun.outcome() != ItemState.SUCCEEDED || !"DRY_RUN".equals(dryRun.code())) {
                return Health.down().withDetail("code", "PROVIDER_DRY_RUN_FAILED")
                        .withDetail("providerCode", dryRun.code()).build();
            }
            return Health.up()
                    .withDetail("databaseVersion", current.getVersion().getVersion())
                    .withDetail("providerCount", providers.providers().size())
                    .withDetail("dryRun", dryRun.code())
                    .build();
        } catch (Exception problem) {
            return Health.down(problem).withDetail("code", "INSTALLATION_CHECK_FAILED").build();
        }
    }

    private static ProviderResult dryRun(ProviderFactory provider) throws Exception {
        ConfigDocument account = json("installation-account", """
                {"baseUrl":"http://127.0.0.1:9"}
                """);
        ConfigDocument message = json("installation-message", """
                {"method":"POST","path":"/installation-check","bodyTemplate":"{}"}
                """);
        Instant now = Instant.now();
        RunExecutionSpec spec = new RunExecutionSpec("installation-check",
                new ProviderRef(HttpProviderFactory.PROVIDER_ID, HttpProviderFactory.VERSION),
                account, message, ExecutionPolicies.defaults(), Map.of("probe", "installation"), true, now);
        try (ProviderSession session = provider.open(new ProviderOpenContext(spec,
                ref -> { throw new IllegalStateException("Installation Dry Run resolved a SecretRef"); },
                ExecutionClock.system()))) {
            RecipientRecord recipient = new RecipientRecord("probe", 0, Map.of());
            return session.send(new ProviderSendRequest(spec.runId(), recipient.itemId(), 1,
                    recipient, message, "installation-check:probe", now.plusSeconds(5)), () -> false);
        }
    }

    private static ConfigDocument json(String schema, String content) {
        return new ConfigDocument(schema, "1", content.getBytes(StandardCharsets.UTF_8));
    }
}

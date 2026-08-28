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
import jakarta.mail.Transport;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

public final class SmtpProviderFactory implements ProviderFactory {
    public static final String PROVIDER_ID = "wepush.email.smtp";
    public static final String VERSION = "0.1.0";

    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            PROVIDER_ID, "SMTP Email", VERSION, 1,
            Set.of(ProviderDescriptor.Capability.DRY_RUN),
            ProviderDescriptor.ThreadSafetyMode.SERIALIZED, 1, Duration.ofSeconds(60),
            StandardProviderSupport.schema(SmtpProviderFactory.class, PROVIDER_ID, "smtp", "account"),
            StandardProviderSupport.schema(SmtpProviderFactory.class, PROVIDER_ID, "smtp", "message"),
            StandardProviderSupport.schema(SmtpProviderFactory.class, PROVIDER_ID, "smtp", "recipient"));

    @Override
    public ProviderDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ValidationResult validateAccount(ConfigDocument account) {
        return StandardProviderSupport.validation(() -> SmtpProviderConfig.account(account));
    }

    @Override
    public ValidationResult validateMessage(ConfigDocument message) {
        return StandardProviderSupport.validation(() -> SmtpProviderConfig.message(message));
    }

    @Override
    public ConnectionTestResult testConnection(ConfigDocument accountDocument, SecretResolver secrets,
                                               Duration timeout) {
        Instant started = Instant.now();
        Transport transport = null;
        try {
            SmtpProviderConfig.Account account = SmtpProviderConfig.account(accountDocument);
            var session = SmtpProviderSession.mailSession(account, timeout);
            transport = session.getTransport("smtp");
            connect(transport, account, secrets);
            return new ConnectionTestResult(true, "SMTP_CONNECTED", "",
                    Duration.between(started, Instant.now()));
        } catch (Exception problem) {
            return new ConnectionTestResult(false, SmtpProviderSession.connectionCode(problem),
                    problem.getClass().getSimpleName(), Duration.between(started, Instant.now()));
        } finally {
            if (transport != null) try { transport.close(); } catch (Exception ignored) { }
        }
    }

    @Override
    public ProviderSession open(ProviderOpenContext context) {
        return new SmtpProviderSession(SmtpProviderConfig.account(context.spec().accountConfig()),
                SmtpProviderConfig.message(context.spec().messageConfig()), context.secretResolver(),
                context.clock(), context.spec().dryRun());
    }

    static void connect(Transport transport, SmtpProviderConfig.Account account,
                        SecretResolver secrets) throws Exception {
        if (account.password() == null) {
            transport.connect(account.host(), account.port(), null, null);
            return;
        }
        try (SecretValue secret = secrets.resolve(account.password())) {
            if (secret == null) throw new IllegalArgumentException("SecretResolver returned null");
            char[] password = secret.copyChars();
            try {
                transport.connect(account.host(), account.port(), account.username(), new String(password));
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }
}

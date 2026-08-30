package com.fangxuele.wepush.next.plugin.carriersms;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

public final class CarrierSmsProviderFactory implements ProviderFactory {
    public static final String VERSION = "1.1.0";
    private final CarrierProtocol protocol;
    private final ProviderDescriptor descriptor;

    public CarrierSmsProviderFactory(CarrierProtocol protocol) {
        this.protocol = java.util.Objects.requireNonNull(protocol, "protocol");
        descriptor = new ProviderDescriptor(protocol.providerId(), protocol + " Carrier SMS", VERSION, 1,
                Set.of(ProviderDescriptor.Capability.DRY_RUN),
                ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE, 256, Duration.ofSeconds(30),
                CarrierProviderSupport.schema(getClass(), protocol, "account"),
                CarrierProviderSupport.schema(getClass(), protocol, "message"),
                CarrierProviderSupport.schema(getClass(), protocol, "recipient"));
    }

    @Override
    public ProviderDescriptor descriptor() { return descriptor; }

    @Override
    public ValidationResult validateAccount(ConfigDocument account) {
        try { CarrierSmsConfig.parse(protocol, account); return ValidationResult.valid(); }
        catch (CarrierProviderProblem problem) {
            return ValidationResult.invalid(problem.path(), problem.code(), problem.getMessage());
        }
    }

    @Override
    public ValidationResult validateMessage(ConfigDocument message) {
        try { CarrierSmsConfig.message(message); return ValidationResult.valid(); }
        catch (CarrierProviderProblem problem) {
            return ValidationResult.invalid(problem.path(), problem.code(), problem.getMessage());
        }
    }

    @Override
    public ConnectionTestResult testConnection(ConfigDocument document, SecretResolver secrets, Duration timeout) {
        Instant started = Instant.now();
        CarrierSmsConfig config;
        try { config = CarrierSmsConfig.parse(protocol, document); }
        catch (CarrierProviderProblem problem) {
            return new ConnectionTestResult(false, problem.code(), problem.getMessage(), Duration.between(started, Instant.now()));
        }
        char[] password = null;
        try (SecretValue value = secrets.resolve(config.password())) {
            password = value.copyChars();
            try (SmsClientGateway gateway = new SmsClientGateway("connection-test-" + System.nanoTime(),
                    config, new String(password))) {
                gateway.connect(Math.toIntExact(Math.min(timeout.toMillis(), config.requestTimeoutMillis())));
            }
            return new ConnectionTestResult(true, protocol + "_LOGIN_ACCEPTED",
                    "Gateway accepted the protocol login", Duration.between(started, Instant.now()));
        } catch (Exception problem) {
            return new ConnectionTestResult(false, protocol + "_LOGIN_FAILED",
                    "Gateway login failed: " + problem.getClass().getSimpleName(),
                    Duration.between(started, Instant.now()));
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
    }

    @Override
    public ProviderSession open(ProviderOpenContext context) throws Exception {
        CarrierSmsConfig account = CarrierSmsConfig.parse(protocol, context.spec().accountConfig());
        String content = CarrierSmsConfig.message(context.spec().messageConfig());
        if (context.spec().dryRun()) return CarrierSmsProviderSession.dryRun(account, content, context);
        char[] password = null;
        try (SecretValue value = context.secretResolver().resolve(account.password())) {
            password = value.copyChars();
            SmsClientGateway gateway = new SmsClientGateway(context.spec().runId(), account, new String(password));
            try {
                gateway.connect(account.requestTimeoutMillis());
                return CarrierSmsProviderSession.live(account, content, context, gateway);
            } catch (Exception problem) {
                gateway.close();
                throw problem;
            }
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
    }
}

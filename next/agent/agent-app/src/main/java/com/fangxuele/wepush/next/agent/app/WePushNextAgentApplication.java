package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.agent.runtime.FileAgentEventOutbox;
import com.fangxuele.wepush.next.agent.runtime.FileAgentCompletionOutbox;
import com.fangxuele.wepush.next.agent.runtime.FileAgentJournal;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WePushNextAgentApplication {
    private WePushNextAgentApplication() {
    }

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        if (args.length > 0) {
            if (args.length == 2 && "--verify-plugin".equals(args[0])) {
                verifyPlugin(Path.of(args[1]), mapper);
                return;
            }
            System.err.println("Usage: wepush-agent [--verify-plugin provider-plugin.zip]");
            System.exit(2);
            return;
        }
        List<ProviderFactory> builtInProviders = ServiceLoader.load(ProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        SignedProviderPluginManager pluginManager = new SignedProviderPluginManager(
                Path.of(System.getenv().getOrDefault("WEPUSH_PLUGIN_ACTIVE_PATH", "plugins/active")),
                Boolean.parseBoolean(System.getenv().getOrDefault("WEPUSH_PLUGIN_DEVELOPER_MODE", "false")),
                System.getenv("WEPUSH_PLUGIN_TRUSTED_KEYS"), mapper);
        List<ProviderFactory> providers = Stream.concat(builtInProviders.stream(),
                pluginManager.load().stream()).toList();
        if (providers.stream().map(provider -> provider.descriptor().providerId() + "@"
                        + provider.descriptor().implementationVersion()).distinct().count()
                != providers.size()) {
            throw new IllegalStateException("Duplicate built-in or plugin Provider version");
        }
        if (providers.isEmpty()) {
            System.err.println("WePush Next Agent cannot start: no Provider was discovered");
            System.exit(2);
        }
        String requestedId = System.getenv().getOrDefault("WEPUSH_AGENT_ID", "local-agent");
        int maximumRuns = parseMaximumRuns(System.getenv("WEPUSH_AGENT_MAXIMUM_RUNS"));
        String serviceHost = System.getenv().getOrDefault("WEPUSH_SERVICE_HOST", "127.0.0.1");
        int servicePort = parsePort(System.getenv("WEPUSH_AGENT_GRPC_PORT"));
        boolean plaintext = Boolean.parseBoolean(
                System.getenv().getOrDefault("WEPUSH_AGENT_GRPC_PLAINTEXT", "true"));
        String legacyToken = System.getenv().getOrDefault("WEPUSH_AGENT_GRPC_TOKEN", "");
        Path journalPath = Path.of(System.getenv().getOrDefault(
                "WEPUSH_AGENT_STATE_PATH", ".local/agent/agent-state.properties"));
        Path eventOutboxPath = Path.of(System.getenv().getOrDefault(
                "WEPUSH_AGENT_EVENT_OUTBOX_PATH", journalPath.resolveSibling("event-outbox.bin").toString()));
        Path completionOutboxPath = Path.of(System.getenv().getOrDefault(
                "WEPUSH_AGENT_COMPLETION_OUTBOX_PATH",
                journalPath.resolveSibling("completion-outbox.bin").toString()));
        Path identityPath = Path.of(System.getenv().getOrDefault(
                "WEPUSH_AGENT_IDENTITY_PATH", journalPath.resolveSibling("identity.json").toString()));
        String serviceBaseUrl = System.getenv().getOrDefault("WEPUSH_SERVICE_BASE_URL",
                "http://" + serviceHost + ":" + parseHttpPort(System.getenv("WEPUSH_SERVICE_HTTP_PORT")));
        AgentIdentityFiles.IdentityMaterial identity = new AgentIdentityFiles(mapper,
                HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build(),
                Clock.systemUTC(), new SecureRandom()).resolve(identityPath, serviceBaseUrl,
                requestedId, System.getenv("WEPUSH_AGENT_ENROLLMENT_TOKEN"), legacyToken);
        String configuredId = identity.agentId();
        String token = identity.credential();
        GrpcAgentClient.TlsConfiguration tls = identity.enrolled()
                ? identity.tls() : explicitTls();
        long eventOutboxBytes = parsePositiveLong(System.getenv("WEPUSH_AGENT_EVENT_OUTBOX_BYTES"),
                FileAgentEventOutbox.DEFAULT_MAXIMUM_BYTES, "WEPUSH_AGENT_EVENT_OUTBOX_BYTES");
        List<ProviderCapability> capabilities = providers.stream()
                .map(ProviderFactory::descriptor)
                .map(WePushNextAgentApplication::capability)
                .toList();

        AtomicBoolean running = new AtomicBoolean(true);
        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("wepush-agent-shutdown").unstarted(() -> {
            running.set(false);
            mainThread.interrupt();
        }));
        try (pluginManager;
             AgentRuntime runtime = new AgentRuntime(
                new AgentId(configuredId),
                productVersion(),
                maximumRuns,
                new DefaultExecutionEngine(providers),
                new FileAgentJournal(journalPath));
             GrpcAgentClient client = new GrpcAgentClient(
                     serviceHost, servicePort, plaintext, token, 1_048_576, tls);
             RemoteAgentRunExecutor remoteRuns = new RemoteAgentRunExecutor(
                     configuredId, runtime, mapper, token,
                     new FileAgentEventOutbox(eventOutboxPath, eventOutboxBytes),
                     new FileAgentCompletionOutbox(completionOutboxPath))) {
            System.out.printf(
                    "WePush Next Agent starting: id=%s service=%s:%d providers=%d maximumRuns=%d%n",
                    configuredId, serviceHost, servicePort, capabilities.size(), maximumRuns);
            long backoffMillis = 1_000;
            while (running.get()) {
                try {
                    client.runSession(runtime, remoteRuns, capabilities);
                    backoffMillis = 1_000;
                } catch (GrpcAgentClient.AgentConnectionException problem) {
                    if (!running.get()) break;
                    System.err.printf("Agent connection lost: %s; reconnecting%n",
                            rootMessage(problem));
                }
                if (!running.get()) break;
                long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, backoffMillis / 3));
                try {
                    Thread.sleep(backoffMillis + jitter);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoffMillis = Math.min(30_000, backoffMillis * 2);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    static void verifyPlugin(Path archive, ObjectMapper mapper) {
        String trustedKeys = System.getenv("WEPUSH_PLUGIN_TRUSTED_KEYS");
        SignedProviderPluginManager verifier = new SignedProviderPluginManager(
                archive.toAbsolutePath().normalize().getParent(), false,
                trustedKeys, mapper);
        Path isolatedDirectory = null;
        try {
            SignedProviderPluginManager.VerifiedPlugin plugin = verifier.verify(archive.toAbsolutePath().normalize());
            isolatedDirectory = Files.createTempDirectory("wepush-provider-verification-");
            Files.copy(archive.toAbsolutePath().normalize(), isolatedDirectory.resolve(plugin.canonicalName()),
                    StandardCopyOption.COPY_ATTRIBUTES);
            List<ProviderFactory> factories;
            try (SignedProviderPluginManager loader = new SignedProviderPluginManager(
                    isolatedDirectory, false, trustedKeys, mapper)) {
                factories = loader.load();
            }
            if (factories.size() != 1
                    || !plugin.version().equals(factories.getFirst().descriptor().implementationVersion())) {
                throw new IllegalStateException("Provider plugin must expose exactly one matching Provider factory");
            }
            System.out.printf("{\"valid\":true,\"pluginId\":\"%s\",\"version\":\"%s\","
                            + "\"canonicalName\":\"%s\",\"providers\":1}%n",
                    plugin.pluginId(), plugin.version(), plugin.canonicalName());
        } catch (Exception problem) {
            throw new IllegalStateException("Provider plugin verification failed: " + rootMessage(problem), problem);
        } finally {
            verifier.close();
            SignedProviderPluginManager.deleteDirectory(isolatedDirectory);
        }
    }

    private static String productVersion() {
        String version = WePushNextAgentApplication.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }

    private static ProviderCapability capability(ProviderDescriptor descriptor) {
        return new ProviderCapability(
                descriptor.providerId(),
                descriptor.implementationVersion(),
                descriptor.spiMajorVersion(),
                descriptor.maximumConcurrency());
    }

    private static int parseMaximumRuns(String value) {
        if (value == null || value.isBlank()) {
            return Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        }
        int parsed = Integer.parseInt(value);
        if (parsed < 1) {
            throw new IllegalArgumentException("WEPUSH_AGENT_MAXIMUM_RUNS must be positive");
        }
        return parsed;
    }

    private static int parsePort(String value) {
        if (value == null || value.isBlank()) return 19090;
        int parsed = Integer.parseInt(value);
        if (parsed < 1 || parsed > 65_535) {
            throw new IllegalArgumentException("WEPUSH_AGENT_GRPC_PORT must be between 1 and 65535");
        }
        return parsed;
    }

    private static int parseHttpPort(String value) {
        if (value == null || value.isBlank()) return 18990;
        int parsed = Integer.parseInt(value);
        if (parsed < 1 || parsed > 65_535) {
            throw new IllegalArgumentException("WEPUSH_SERVICE_HTTP_PORT must be between 1 and 65535");
        }
        return parsed;
    }

    private static GrpcAgentClient.TlsConfiguration explicitTls() {
        return new GrpcAgentClient.TlsConfiguration(
                optionalPath(System.getenv("WEPUSH_AGENT_GRPC_CA_CERT")),
                optionalPath(System.getenv("WEPUSH_AGENT_GRPC_CLIENT_CERT")),
                optionalPath(System.getenv("WEPUSH_AGENT_GRPC_CLIENT_KEY")));
    }

    private static Path optionalPath(String value) {
        return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize();
    }

    private static long parsePositiveLong(String value, long defaultValue, String name) {
        if (value == null || value.isBlank()) return defaultValue;
        long parsed = Long.parseLong(value);
        if (parsed < 1) throw new IllegalArgumentException(name + " must be positive");
        return parsed;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}

package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.agent.runtime.FileAgentJournal;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WePushNextAgentApplication {
    private WePushNextAgentApplication() {
    }

    public static void main(String[] args) {
        List<ProviderFactory> providers = ServiceLoader.load(ProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        if (providers.isEmpty()) {
            System.err.println("WePush Next Agent cannot start: no Provider was discovered");
            System.exit(2);
        }
        String configuredId = System.getenv().getOrDefault("WEPUSH_AGENT_ID", "local-agent");
        int maximumRuns = parseMaximumRuns(System.getenv("WEPUSH_AGENT_MAXIMUM_RUNS"));
        String serviceHost = System.getenv().getOrDefault("WEPUSH_SERVICE_HOST", "127.0.0.1");
        int servicePort = parsePort(System.getenv("WEPUSH_AGENT_GRPC_PORT"));
        boolean plaintext = Boolean.parseBoolean(
                System.getenv().getOrDefault("WEPUSH_AGENT_GRPC_PLAINTEXT", "true"));
        String token = System.getenv().getOrDefault("WEPUSH_AGENT_GRPC_TOKEN", "");
        Path journalPath = Path.of(System.getenv().getOrDefault(
                "WEPUSH_AGENT_STATE_PATH", ".local/agent/agent-state.properties"));
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
        try (AgentRuntime runtime = new AgentRuntime(
                new AgentId(configuredId),
                "0.1.0-SNAPSHOT",
                maximumRuns,
                new DefaultExecutionEngine(providers),
                new FileAgentJournal(journalPath));
             GrpcAgentClient client = new GrpcAgentClient(
                     serviceHost, servicePort, plaintext, token, 1_048_576);
             RemoteAgentRunExecutor remoteRuns = new RemoteAgentRunExecutor(
                     runtime, new ObjectMapper().findAndRegisterModules(), token)) {
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

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}

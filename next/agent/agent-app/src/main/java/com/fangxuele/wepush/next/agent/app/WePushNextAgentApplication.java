package com.fangxuele.wepush.next.agent.app;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.agent.runtime.InMemoryAgentJournal;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;

import java.util.List;
import java.util.ServiceLoader;

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
        List<ProviderCapability> capabilities = providers.stream()
                .map(ProviderFactory::descriptor)
                .map(WePushNextAgentApplication::capability)
                .toList();

        try (AgentRuntime runtime = new AgentRuntime(
                new AgentId(configuredId),
                "0.1.0-SNAPSHOT",
                maximumRuns,
                new DefaultExecutionEngine(providers),
                new InMemoryAgentJournal())) {
            AgentFrames.AgentToService hello = runtime.hello(capabilities);
            System.out.printf(
                    "WePush Next Agent ready: id=%s sequence=%d providers=%d maximumRuns=%d%n",
                    hello.agentId(), hello.sequence(), capabilities.size(), maximumRuns);
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
}

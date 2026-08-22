package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import com.fangxuele.wepush.next.service.domain.AgentRegistration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agents")
final class AgentController {
    private final AgentApplicationService agents;

    AgentController(AgentApplicationService agents) {
        this.agents = agents;
    }

    @GetMapping
    List<ControlPlaneApi.AgentResponse> list() {
        return agents.list().stream().map(AgentController::response).toList();
    }

    @GetMapping("/{agentId}")
    ControlPlaneApi.AgentResponse get(@PathVariable String agentId) {
        return response(agents.get(agentId));
    }

    private static ControlPlaneApi.AgentResponse response(AgentRegistration agent) {
        List<ControlPlaneApi.AgentProviderResponse> providers = agent.providers().stream()
                .map(provider -> new ControlPlaneApi.AgentProviderResponse(provider.providerId(),
                        provider.implementationVersion(), provider.spiMajor(),
                        provider.maximumConcurrency()))
                .toList();
        return new ControlPlaneApi.AgentResponse(agent.id(), agent.status().name(),
                agent.agentVersion(), agent.protocolVersion(), agent.operatingSystem(),
                agent.architecture(), agent.javaVersion(), agent.maximumRuns(), agent.activeRuns(),
                agent.availableRuns(), providers, agent.sessionId(), agent.lastAgentSequence(),
                agent.lastServiceSequence(), agent.connectedAt(), agent.lastSeenAt(),
                agent.disconnectedAt(), agent.version(),
                Map.of("self", "/api/v1/agents/" + agent.id()));
    }
}

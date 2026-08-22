package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
final class AgentOfflineScheduler {
    private final AgentApplicationService agents;

    AgentOfflineScheduler(AgentApplicationService agents) {
        this.agents = agents;
    }

    @Scheduled(fixedDelayString = "${wepush.agent.grpc.heartbeat-interval:PT10S}")
    void expireSilentAgents() {
        agents.expireSilent();
    }
}

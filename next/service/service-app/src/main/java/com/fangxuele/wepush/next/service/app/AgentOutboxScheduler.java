package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RemoteRunCoordinator;
import org.springframework.scheduling.annotation.Scheduled;

/** Retries durable Service-to-Agent messages across Service instances. */
final class AgentOutboxScheduler {
    private final RemoteRunCoordinator remoteRuns;
    private final boolean enabled;

    AgentOutboxScheduler(RemoteRunCoordinator remoteRuns, boolean enabled,
                         PostgresNotificationBus notifications) {
        this.remoteRuns = remoteRuns;
        this.enabled = enabled;
        if (enabled) notifications.subscribe(PostgresNotificationBus.AGENT_OUTBOX, this::scan);
    }

    @Scheduled(fixedDelayString = "${wepush.agent.outbox-scan-interval:PT1S}")
    void scan() {
        if (enabled) remoteRuns.deliverPending();
    }
}

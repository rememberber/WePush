package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RunApplicationService;
import org.springframework.scheduling.annotation.Scheduled;

/** Cross-instance SSE reconciliation backed by the durable run event log. */
final class RunEventPoller {
    private final LocalRunEventHub events;
    private final RunApplicationService runs;

    RunEventPoller(LocalRunEventHub events, RunApplicationService runs,
                   PostgresNotificationBus notifications) {
        this.events = events;
        this.runs = runs;
        notifications.subscribe(PostgresNotificationBus.RUN_EVENT, this::poll);
    }

    @Scheduled(fixedDelayString = "${wepush.events.poll-interval:PT1S}")
    void poll() {
        events.poll(runs);
    }
}

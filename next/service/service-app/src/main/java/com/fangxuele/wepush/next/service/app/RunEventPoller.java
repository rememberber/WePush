package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RunApplicationService;
import org.springframework.scheduling.annotation.Scheduled;

/** Cross-instance SSE reconciliation backed by the durable run event log. */
final class RunEventPoller {
    private final LocalRunEventHub events;
    private final RunApplicationService runs;

    RunEventPoller(LocalRunEventHub events, RunApplicationService runs) {
        this.events = events;
        this.runs = runs;
    }

    @Scheduled(fixedDelayString = "${wepush.events.poll-interval:PT1S}")
    void poll() {
        events.poll(runs);
    }
}

package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RemoteRunCoordinator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wepush.execution.mode", havingValue = "remote")
final class RemoteLeaseScheduler {
    private final RemoteRunCoordinator remoteRuns;

    RemoteLeaseScheduler(RemoteRunCoordinator remoteRuns) {
        this.remoteRuns = remoteRuns;
    }

    @Scheduled(fixedDelayString = "${wepush.agent.lease-scan-interval:PT10S}")
    void scan() {
        remoteRuns.expireAndRecover();
    }
}

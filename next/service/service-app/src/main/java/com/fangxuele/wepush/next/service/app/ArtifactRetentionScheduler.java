package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.ArtifactApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
final class ArtifactRetentionScheduler {
    private final ArtifactApplicationService artifacts;

    ArtifactRetentionScheduler(ArtifactApplicationService artifacts) {
        this.artifacts = artifacts;
    }

    @Scheduled(initialDelayString = "${wepush.artifact.retention-initial-delay:PT1M}",
            fixedDelayString = "${wepush.artifact.retention-interval:PT1H}")
    void cleanupExpiredArtifacts() {
        artifacts.cleanupExpired(100);
    }
}

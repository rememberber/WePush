package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.SystemInfoResponse;
import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.ArtifactApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
final class SystemController {
    private final String mode;
    private final ArtifactApplicationService artifacts;

    SystemController(@Value("${wepush.mode:standalone}") String mode,
                     ArtifactApplicationService artifacts) {
        this.mode = mode;
        this.artifacts = artifacts;
    }

    @GetMapping("/info")
    SystemInfoResponse info() {
        return new SystemInfoResponse("WePush Next", "0.1.0-SNAPSHOT", mode, Instant.now());
    }

    @PostMapping("/maintenance/artifacts/retention")
    ControlPlaneApi.ArtifactCleanupResponse cleanupArtifacts(
            @RequestParam(defaultValue = "100") int limit) {
        ArtifactApplicationService.CleanupResult result = artifacts.cleanupExpired(limit);
        return new ControlPlaneApi.ArtifactCleanupResponse(
                result.claimed(), result.deleted(), result.failed());
    }
}

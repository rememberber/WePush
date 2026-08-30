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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
final class SystemController {
    private final String mode;
    private final ArtifactApplicationService artifacts;
    private final SystemOperationsService operations;

    SystemController(@Value("${wepush.mode:standalone}") String mode,
                     ArtifactApplicationService artifacts, SystemOperationsService operations) {
        this.mode = mode;
        this.artifacts = artifacts;
        this.operations = operations;
    }

    @GetMapping("/info")
    SystemInfoResponse info() {
        return new SystemInfoResponse("WePush Next", productVersion(), mode, Instant.now());
    }

    static String productVersion() {
        String version = SystemController.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }

    @PostMapping("/diagnostics")
    ResponseEntity<byte[]> diagnostics() {
        byte[] bundle = operations.diagnosticBundle(productVersion(), mode);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=wepush-next-diagnostics-" + Instant.now().toEpochMilli() + ".zip")
                .contentLength(bundle.length).body(bundle);
    }

    @PostMapping("/version-check")
    SystemOperationsService.VersionCheck versionCheck() {
        return operations.versionCheck(productVersion());
    }

    @PostMapping("/maintenance/artifacts/retention")
    ControlPlaneApi.ArtifactCleanupResponse cleanupArtifacts(
            @RequestParam(defaultValue = "100") int limit) {
        ArtifactApplicationService.CleanupResult result = artifacts.cleanupExpired(limit);
        return new ControlPlaneApi.ArtifactCleanupResponse(
                result.claimed(), result.deleted(), result.failed());
    }
}

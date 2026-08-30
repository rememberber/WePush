package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.SystemInfoResponse;

import java.io.InputStream;
import java.time.Instant;

public final class SystemClient {
    private final HttpTransport transport;

    SystemClient(HttpTransport transport) {
        this.transport = transport;
    }

    public SystemInfoResponse info() {
        return transport.getJson("/api/v1/system/info", SystemInfoResponse.class);
    }

    public String health() {
        return transport.getJson("/actuator/health", HealthResponse.class).status();
    }

    public VersionCheck checkVersion() {
        return transport.postJson("/api/v1/system/version-check", VersionCheck.class);
    }

    /** Generates the bundle on demand; the caller owns and must close the returned stream. */
    public InputStream downloadRedactedDiagnostics() {
        return transport.postStream("/api/v1/system/diagnostics");
    }

    private record HealthResponse(String status) {
    }

    public record VersionCheck(boolean successful, String currentVersion, String latestVersion,
                               boolean updateAvailable, String releaseUrl, Instant checkedAt,
                               String diagnostic) { }
}

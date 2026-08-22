package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.SystemInfoResponse;

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

    private record HealthResponse(String status) {
    }
}

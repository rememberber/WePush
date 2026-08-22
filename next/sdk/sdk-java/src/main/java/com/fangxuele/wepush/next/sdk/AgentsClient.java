package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;

import java.util.List;

public final class AgentsClient {
    private final HttpTransport transport;

    AgentsClient(HttpTransport transport) {
        this.transport = transport;
    }

    public List<ControlPlaneApi.AgentResponse> list() {
        ControlPlaneApi.AgentResponse[] agents = transport.getJson(
                "/api/v1/agents", ControlPlaneApi.AgentResponse[].class);
        return List.of(agents);
    }

    public ControlPlaneApi.AgentResponse get(String agentId) {
        return transport.getJson("/api/v1/agents/" + pathId(agentId),
                ControlPlaneApi.AgentResponse.class);
    }

    private static String pathId(String value) {
        if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("agentId contains unsupported path characters");
        }
        return value;
    }
}

package com.fangxuele.wepush.next.sdk;

import java.time.Instant;
import java.util.List;

public final class WorkspacesClient {
    private final HttpTransport transport;

    WorkspacesClient(HttpTransport transport) {
        this.transport = transport;
    }

    public List<Workspace> list() {
        return List.of(transport.getJson("/api/v1/workspaces", Workspace[].class));
    }

    public Workspace create(String name) {
        return transport.postJson("/api/v1/workspaces", new CreateWorkspace(name), null, Workspace.class);
    }

    public Workspace get(String workspaceId) {
        if (workspaceId == null || !workspaceId.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("workspaceId contains unsupported path characters");
        }
        return transport.getJson("/api/v1/workspaces/" + workspaceId, Workspace.class);
    }

    public WorkspacePolicy policy(String workspaceId) {
        return transport.getJson(path(workspaceId) + "/policy", WorkspacePolicy.class);
    }

    public WorkspacePolicy updatePolicy(String workspaceId, UpdateWorkspacePolicy policy) {
        return transport.putJson(path(workspaceId) + "/policy", policy, WorkspacePolicy.class);
    }

    private static String path(String workspaceId) {
        if (workspaceId == null || !workspaceId.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("workspaceId contains unsupported path characters");
        }
        return "/api/v1/workspaces/" + workspaceId;
    }

    private record CreateWorkspace(String name) {}
    public record Workspace(String id, String name, String status, Instant createdAt, long version) {}
    public record UpdateWorkspacePolicy(int maxAgents, int maxConcurrentRuns, int maxTotalConcurrency,
                                        long artifactQuotaBytes, long artifactRetentionSeconds) { }
    public record WorkspacePolicy(String workspaceId, int maxAgents, int maxConcurrentRuns,
                                  int maxTotalConcurrency, long artifactQuotaBytes,
                                  long artifactRetentionSeconds, long usedAgents, long activeRuns,
                                  long usedConcurrency, long usedArtifactBytes, Instant updatedAt,
                                  long version) { }
}

package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.time.Instant;

public final class WorkspaceClient {
    private final HttpTransport transport;
    private final String base;

    WorkspaceClient(HttpTransport transport, String workspaceId) {
        if (workspaceId == null || !workspaceId.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("workspaceId contains unsupported path characters");
        }
        this.transport = transport;
        this.base = "/api/v1/workspaces/" + workspaceId;
    }

    public List<ControlPlaneApi.AccountResponse> accounts() {
        return List.of(transport.getJson(base + "/accounts", ControlPlaneApi.AccountResponse[].class));
    }

    public ControlPlaneApi.AccountResponse createAccount(ControlPlaneApi.CreateAccountRequest request) {
        return transport.postJson(base + "/accounts", request, null, ControlPlaneApi.AccountResponse.class);
    }

    public List<ControlPlaneApi.MessageResponse> messages() {
        return List.of(transport.getJson(base + "/messages", ControlPlaneApi.MessageResponse[].class));
    }

    public ControlPlaneApi.MessageResponse createMessage(ControlPlaneApi.CreateMessageRequest request) {
        return transport.postJson(base + "/messages", request, null, ControlPlaneApi.MessageResponse.class);
    }

    public List<ControlPlaneApi.AudienceResponse> audiences() {
        return List.of(transport.getJson(base + "/audiences", ControlPlaneApi.AudienceResponse[].class));
    }

    public ControlPlaneApi.AudienceResponse createAudience(ControlPlaneApi.CreateAudienceRequest request) {
        return transport.postJson(base + "/audiences", request, null, ControlPlaneApi.AudienceResponse.class);
    }

    public List<ControlPlaneApi.JobResponse> jobs() {
        return List.of(transport.getJson(base + "/jobs", ControlPlaneApi.JobResponse[].class));
    }

    public ControlPlaneApi.JobResponse createJob(ControlPlaneApi.CreateJobRequest request) {
        return transport.postJson(base + "/jobs", request, null, ControlPlaneApi.JobResponse.class);
    }

    public List<ControlPlaneApi.RunResponse> runs() {
        return List.of(transport.getJson(base + "/runs", ControlPlaneApi.RunResponse[].class));
    }

    public ControlPlaneApi.RunResponse run(String runId) {
        return transport.getJson(base + "/runs/" + pathId(runId), ControlPlaneApi.RunResponse.class);
    }

    public ControlPlaneApi.RunResponse createRun(String jobId, String idempotencyKey,
                                                 ControlPlaneApi.CreateRunRequest request) {
        return transport.postJson(base + "/jobs/" + pathId(jobId) + "/runs", request,
                idempotencyKey, ControlPlaneApi.RunResponse.class);
    }

    public String runEventsPath(String runId) {
        return base + "/runs/" + pathId(runId) + "/events";
    }

    public ControlPlaneApi.RunItemResultPage runItems(String runId, String cursor, int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
        String path = base + "/runs/" + pathId(runId) + "/items?limit=" + limit;
        if (cursor != null && !cursor.isBlank()) {
            if (!cursor.matches("[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException("cursor contains unsupported characters");
            }
            path += "&cursor=" + cursor;
        }
        return transport.getJson(path, ControlPlaneApi.RunItemResultPage.class);
    }

    public List<ControlPlaneApi.ArtifactResponse> runArtifacts(String runId) {
        return List.of(transport.getJson(base + "/runs/" + pathId(runId) + "/artifacts",
                ControlPlaneApi.ArtifactResponse[].class));
    }

    public ControlPlaneApi.ArtifactResponse createResultExport(String runId) {
        return transport.postJson(base + "/runs/" + pathId(runId) + "/artifacts/result-export",
                Map.of(), null, ControlPlaneApi.ArtifactResponse.class);
    }

    public ControlPlaneApi.ArtifactResponse artifact(String artifactId) {
        return transport.getJson(base + "/artifacts/" + pathId(artifactId),
                ControlPlaneApi.ArtifactResponse.class);
    }

    /** The caller owns and must close the returned stream. */
    public InputStream downloadArtifact(String artifactId) {
        return transport.getStream(base + "/artifacts/" + pathId(artifactId) + "/content");
    }

    public ControlPlaneApi.RunCommandResponse pauseRun(String runId, String idempotencyKey) {
        return runCommand(runId, "pause", Map.of(), idempotencyKey);
    }

    public ControlPlaneApi.RunCommandResponse resumeRun(String runId, String idempotencyKey) {
        return runCommand(runId, "resume", Map.of(), idempotencyKey);
    }

    public ControlPlaneApi.RunCommandResponse cancelRun(String runId, String reason, String idempotencyKey) {
        return runCommand(runId, "cancel", new ControlPlaneApi.CancelRunRequest(reason), idempotencyKey);
    }

    public ControlPlaneApi.RunCommandResponse changeRunConcurrency(
            String runId, int target, String idempotencyKey) {
        return runCommand(runId, "concurrency",
                new ControlPlaneApi.ChangeConcurrencyRequest(target), idempotencyKey);
    }

    public ControlPlaneApi.SecretMetadataResponse replaceSecret(
            String namespace, String name, String version, char[] value) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("secret value must not be empty");
        }
        char[] owned = value.clone();
        try {
            return transport.putJson(secretPath(namespace, name, version),
                    new ControlPlaneApi.SecretWriteRequest(owned),
                    ControlPlaneApi.SecretMetadataResponse.class);
        } finally {
            Arrays.fill(owned, '\0');
        }
    }

    public ControlPlaneApi.SecretMetadataResponse secretMetadata(
            String namespace, String name, String version) {
        return transport.getJson(secretPath(namespace, name, version),
                ControlPlaneApi.SecretMetadataResponse.class);
    }

    public List<Schedule> schedules() {
        return List.of(transport.getJson(base + "/schedules", Schedule[].class));
    }

    public Schedule createSchedule(String name, String jobId, String cronExpression,
                                   String timezone, MisfirePolicy misfirePolicy, boolean enabled) {
        return transport.postJson(base + "/schedules", new CreateSchedule(name, jobId,
                cronExpression, timezone, misfirePolicy.name(), enabled), null, Schedule.class);
    }

    public Schedule setScheduleEnabled(String scheduleId, boolean enabled) {
        return transport.patchJson(base + "/schedules/" + pathId(scheduleId),
                Map.of("enabled", enabled), Schedule.class);
    }

    public void deleteSchedule(String scheduleId) {
        transport.delete(base + "/schedules/" + pathId(scheduleId));
    }

    public List<AuditEvent> auditEvents(int limit) {
        if (limit < 1 || limit > 1_000) throw new IllegalArgumentException("limit must be 1..1000");
        return List.of(transport.getJson(base + "/audit-events?limit=" + limit,
                AuditEvent[].class));
    }

    private ControlPlaneApi.RunCommandResponse runCommand(
            String runId, String command, Object request, String idempotencyKey) {
        return transport.postJson(base + "/runs/" + pathId(runId) + "/commands/" + command,
                request, idempotencyKey, ControlPlaneApi.RunCommandResponse.class);
    }

    private String secretPath(String namespace, String name, String version) {
        return base + "/secrets/" + pathId(namespace) + "/" + pathId(name)
                + "/versions/" + pathId(version);
    }

    private static String pathId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("resource ID contains unsupported path characters");
        }
        return id;
    }

    public enum MisfirePolicy { FIRE_ONCE, SKIP }
    private record CreateSchedule(String name, String jobId, String cronExpression,
                                  String timezone, String misfirePolicy, boolean enabled) {}
    public record Schedule(String id, String workspaceId, String jobId, String name,
                           String cronExpression, String timezone, String misfirePolicy,
                           boolean enabled, Instant nextFireAt, Instant lastFireAt,
                           Instant createdAt, Instant updatedAt, long version) {}
    public record AuditEvent(String id, String workspaceId, String actorType, String actorId,
                             String action, String resourceType, String resourceId, String result,
                             String detailsJson, Instant occurredAt) {}
}

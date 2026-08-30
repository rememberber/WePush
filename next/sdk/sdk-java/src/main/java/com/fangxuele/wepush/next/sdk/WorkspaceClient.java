package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

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
        return List.of(transport.getJson(base + "/accounts", AccountPage.class).items());
    }

    public AccountPage accountPage(PageQuery query) {
        return transport.getJson(base + "/accounts" + query.queryString(), AccountPage.class);
    }

    public ControlPlaneApi.AccountResponse createAccount(ControlPlaneApi.CreateAccountRequest request) {
        return transport.postJson(base + "/accounts", request, null, ControlPlaneApi.AccountResponse.class);
    }

    public ControlPlaneApi.AccountResponse updateAccount(String accountId, ControlPlaneApi.UpdateAccountRequest request) {
        return transport.patchJson(base + "/accounts/" + pathId(accountId), request,
                ControlPlaneApi.AccountResponse.class);
    }

    public ControlPlaneApi.ConnectionTestResponse testAccount(String accountId, String timeout) {
        return transport.postJson(base + "/accounts/" + pathId(accountId) + "/connection-test",
                new ControlPlaneApi.ConnectionTestRequest(timeout), null,
                ControlPlaneApi.ConnectionTestResponse.class);
    }

    public AuthenticationCircuit authenticationCircuit(String accountId) {
        return transport.getJson(base + "/accounts/" + pathId(accountId) + "/authentication-circuit",
                AuthenticationCircuit.class);
    }

    public AuthenticationCircuit resetAuthenticationCircuit(String accountId) {
        return transport.deleteJson(base + "/accounts/" + pathId(accountId) + "/authentication-circuit",
                AuthenticationCircuit.class);
    }

    public List<ControlPlaneApi.MessageResponse> messages() {
        return List.of(transport.getJson(base + "/messages", MessagePage.class).items());
    }

    public MessagePage messagePage(PageQuery query) {
        return transport.getJson(base + "/messages" + query.queryString(), MessagePage.class);
    }

    public ControlPlaneApi.MessageResponse createMessage(ControlPlaneApi.CreateMessageRequest request) {
        return transport.postJson(base + "/messages", request, null, ControlPlaneApi.MessageResponse.class);
    }

    public ControlPlaneApi.MessageResponse updateMessage(String messageId, ControlPlaneApi.UpdateMessageRequest request) {
        return transport.patchJson(base + "/messages/" + pathId(messageId), request,
                ControlPlaneApi.MessageResponse.class);
    }

    public ControlPlaneApi.MessageResponse copyMessage(String messageId, String name) {
        return transport.postJson(base + "/messages/" + pathId(messageId) + "/copy",
                new ControlPlaneApi.CopyResourceRequest(name), null, ControlPlaneApi.MessageResponse.class);
    }

    public ControlPlaneApi.RevisionPage messageRevisions(String messageId, int beforeRevision, int limit) {
        return transport.getJson(base + "/messages/" + pathId(messageId) + "/revisions?beforeRevision="
                + beforeRevision + "&limit=" + limit, ControlPlaneApi.RevisionPage.class);
    }

    public ControlPlaneApi.MessageDiffResponse messageDiff(String messageId, int from, int to) {
        return transport.getJson(base + "/messages/" + pathId(messageId) + "/diff?from=" + from + "&to=" + to,
                ControlPlaneApi.MessageDiffResponse.class);
    }

    public List<ControlPlaneApi.AudienceResponse> audiences() {
        return List.of(transport.getJson(base + "/audiences", AudiencePage.class).items());
    }

    public AudiencePage audiencePage(PageQuery query) {
        return transport.getJson(base + "/audiences" + query.queryString(), AudiencePage.class);
    }

    public ControlPlaneApi.AudienceResponse createAudience(ControlPlaneApi.CreateAudienceRequest request) {
        return transport.postJson(base + "/audiences", request, null, ControlPlaneApi.AudienceResponse.class);
    }

    public ControlPlaneApi.AudienceResponse updateAudience(String audienceId,
                                                            ControlPlaneApi.UpdateAudienceRequest request) {
        return transport.patchJson(base + "/audiences/" + pathId(audienceId), request,
                ControlPlaneApi.AudienceResponse.class);
    }

    public ControlPlaneApi.AudienceImportResponse uploadAudience(
            Path file, String name, String audienceId, String format, String itemIdColumn,
            Map<String, String> fieldMapping, String delimiter) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", name);
        if (audienceId != null && !audienceId.isBlank()) fields.put("audienceId", audienceId);
        fields.put("format", format == null ? "CSV" : format);
        if (itemIdColumn != null && !itemIdColumn.isBlank()) fields.put("itemIdColumn", itemIdColumn);
        fields.put("fieldMapping", fieldMapping == null ? Map.of() : fieldMapping);
        if (delimiter != null && !delimiter.isEmpty()) fields.put("delimiter", delimiter);
        return transport.postMultipart(base + "/audience-imports", fields,
                file.getFileName().toString(), file, ControlPlaneApi.AudienceImportResponse.class);
    }

    public ControlPlaneApi.AudienceImportResponse audienceImport(String importId) {
        return transport.getJson(base + "/audience-imports/" + pathId(importId),
                ControlPlaneApi.AudienceImportResponse.class);
    }

    public ControlPlaneApi.AudienceResponse commitAudienceImport(String importId) {
        return transport.postJson(base + "/audience-imports/" + pathId(importId) + "/commit",
                Map.of(), null, ControlPlaneApi.AudienceResponse.class);
    }

    /** The caller owns and must close the returned stream. */
    public InputStream downloadAudienceImportErrors(String importId) {
        return transport.getStream(base + "/audience-imports/" + pathId(importId) + "/errors.csv");
    }

    public List<ControlPlaneApi.JobResponse> jobs() {
        return List.of(transport.getJson(base + "/jobs", JobPage.class).items());
    }

    public JobPage jobPage(PageQuery query) {
        return transport.getJson(base + "/jobs" + query.queryString(), JobPage.class);
    }

    public ControlPlaneApi.JobResponse createJob(ControlPlaneApi.CreateJobRequest request) {
        return transport.postJson(base + "/jobs", request, null, ControlPlaneApi.JobResponse.class);
    }

    public ControlPlaneApi.JobResponse updateJob(String jobId, ControlPlaneApi.UpdateJobRequest request) {
        return transport.patchJson(base + "/jobs/" + pathId(jobId), request, ControlPlaneApi.JobResponse.class);
    }

    public ControlPlaneApi.JobResponse copyJob(String jobId, String name) {
        return transport.postJson(base + "/jobs/" + pathId(jobId) + "/copy",
                new ControlPlaneApi.CopyResourceRequest(name), null, ControlPlaneApi.JobResponse.class);
    }

    public List<ControlPlaneApi.RunResponse> runs() {
        return List.of(transport.getJson(base + "/runs", RunPage.class).items());
    }

    public RunPage runPage(PageQuery query) {
        return transport.getJson(base + "/runs" + query.queryString(), RunPage.class);
    }

    public ControlPlaneApi.RunResponse run(String runId) {
        return transport.getJson(base + "/runs/" + pathId(runId), ControlPlaneApi.RunResponse.class);
    }

    public ControlPlaneApi.RunResponse createRun(String jobId, String idempotencyKey,
                                                 ControlPlaneApi.CreateRunRequest request) {
        return transport.postJson(base + "/jobs/" + pathId(jobId) + "/runs", request,
                idempotencyKey, ControlPlaneApi.RunResponse.class);
    }

    public ControlPlaneApi.LiveConfirmationResponse confirmRun(String jobId) {
        return transport.postJson(base + "/jobs/" + pathId(jobId) + "/run-confirmation", Map.of(), null,
                ControlPlaneApi.LiveConfirmationResponse.class);
    }

    public ControlPlaneApi.RetryConfirmationResponse confirmRetry(String runId, Set<String> states) {
        return transport.postJson(base + "/runs/" + pathId(runId) + "/retry-confirmation",
                new ControlPlaneApi.RetryRequest(states, null), null,
                ControlPlaneApi.RetryConfirmationResponse.class);
    }

    public ControlPlaneApi.RunResponse retryRun(String runId, Set<String> states,
                                                String confirmationToken, String idempotencyKey) {
        return transport.postJson(base + "/runs/" + pathId(runId) + "/retries",
                new ControlPlaneApi.RetryRequest(states, confirmationToken), idempotencyKey,
                ControlPlaneApi.RunResponse.class);
    }

    public ControlPlaneApi.OverviewResponse overview() {
        return transport.getJson(base + "/overview", ControlPlaneApi.OverviewResponse.class);
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
        return List.of(transport.getJson(base + "/schedules", SchedulePage.class).items());
    }

    public SchedulePage schedulePage(PageQuery query) {
        return transport.getJson(base + "/schedules" + query.queryString(), SchedulePage.class);
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

    public Schedule updateSchedule(String scheduleId, UpdateSchedule request) {
        return transport.patchJson(base + "/schedules/" + pathId(scheduleId), request, Schedule.class);
    }

    public void deleteSchedule(String scheduleId) {
        transport.delete(base + "/schedules/" + pathId(scheduleId));
    }

    public List<AuditEvent> auditEvents(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be 1..100");
        return List.of(transport.getJson(base + "/audit-events?limit=" + limit,
                AuditPage.class).items());
    }

    public AuditPage auditPage(PageQuery query) {
        return transport.getJson(base + "/audit-events" + query.queryString(), AuditPage.class);
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
    public record AuthenticationCircuit(String workspaceId, String accountId, int failureRuns,
                                        Instant firstFailureAt, Instant lastFailureAt, Instant openUntil,
                                        String lastRunId, long version) { }

    public record PageQuery(String cursor, int limit, String name, String status, Instant from, Instant to) {
        public PageQuery { if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be 1..100"); }
        String queryString() {
            StringBuilder value = new StringBuilder("?limit=").append(limit);
            parameter(value, "cursor", cursor); parameter(value, "name", name); parameter(value, "status", status);
            parameter(value, "from", from == null ? null : from.toString());
            parameter(value, "to", to == null ? null : to.toString()); return value.toString();
        }
    }
    public record UpdateSchedule(String name, String jobId, String cronExpression, String timezone,
                                 String misfirePolicy, Boolean enabled) { }
    public record AccountPage(ControlPlaneApi.AccountResponse[] items, ControlPlaneApi.CursorPage page) { }
    public record MessagePage(ControlPlaneApi.MessageResponse[] items, ControlPlaneApi.CursorPage page) { }
    public record AudiencePage(ControlPlaneApi.AudienceResponse[] items, ControlPlaneApi.CursorPage page) { }
    public record JobPage(ControlPlaneApi.JobResponse[] items, ControlPlaneApi.CursorPage page) { }
    public record RunPage(ControlPlaneApi.RunResponse[] items, ControlPlaneApi.CursorPage page) { }
    public record SchedulePage(Schedule[] items, ControlPlaneApi.CursorPage page) { }
    public record AuditPage(AuditEvent[] items, ControlPlaneApi.CursorPage page) { }

    private static void parameter(StringBuilder target, String name, String value) {
        if (value != null && !value.isBlank()) target.append('&').append(name).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}

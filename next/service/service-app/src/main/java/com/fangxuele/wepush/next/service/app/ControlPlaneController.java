package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.AccountApplicationService;
import com.fangxuele.wepush.next.service.application.AudienceApplicationService;
import com.fangxuele.wepush.next.service.application.JobApplicationService;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.application.MessageApplicationService;
import com.fangxuele.wepush.next.service.application.RunApplicationService;
import com.fangxuele.wepush.next.service.application.RunCommandApplicationService;
import com.fangxuele.wepush.next.service.application.RunResultApplicationService;
import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
final class ControlPlaneController {
    private final AccountApplicationService accounts;
    private final MessageApplicationService messages;
    private final AudienceApplicationService audiences;
    private final JobApplicationService jobs;
    private final RunApplicationService runs;
    private final RunResultApplicationService results;
    private final RunCommandApplicationService commands;
    private final JsonCodec json;

    ControlPlaneController(AccountApplicationService accounts, MessageApplicationService messages,
                           AudienceApplicationService audiences, JobApplicationService jobs,
                           RunApplicationService runs, RunResultApplicationService results,
                           RunCommandApplicationService commands, JsonCodec json) {
        this.accounts = accounts;
        this.messages = messages;
        this.audiences = audiences;
        this.jobs = jobs;
        this.runs = runs;
        this.results = results;
        this.commands = commands;
        this.json = json;
    }

    @PostMapping("/accounts")
    ResponseEntity<ControlPlaneApi.AccountResponse> createAccount(
            @PathVariable String workspaceId, @RequestBody ControlPlaneApi.CreateAccountRequest request) {
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        AccountDefinition created = accounts.create(workspace, new AccountApplicationService.CreateAccount(
                request.name(), request.providerId(), request.providerVersion(), request.configuration()));
        return created("/api/v1/workspaces/" + workspaceId + "/accounts/" + created.id(), account(created));
    }

    @GetMapping("/accounts")
    List<ControlPlaneApi.AccountResponse> listAccounts(@PathVariable String workspaceId) {
        return accounts.list(new WorkspaceId(workspaceId)).stream().map(this::account).toList();
    }

    @GetMapping("/accounts/{accountId}")
    ControlPlaneApi.AccountResponse getAccount(@PathVariable String workspaceId, @PathVariable String accountId) {
        return account(accounts.get(new WorkspaceId(workspaceId), accountId));
    }

    @PostMapping("/messages")
    ResponseEntity<ControlPlaneApi.MessageResponse> createMessage(
            @PathVariable String workspaceId, @RequestBody ControlPlaneApi.CreateMessageRequest request) {
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        MessageDefinition created = messages.create(workspace, new MessageApplicationService.CreateMessage(
                request.name(), request.providerId(), request.providerVersion(), request.content()));
        return created("/api/v1/workspaces/" + workspaceId + "/messages/" + created.id(), message(created));
    }

    @GetMapping("/messages")
    List<ControlPlaneApi.MessageResponse> listMessages(@PathVariable String workspaceId) {
        return messages.list(new WorkspaceId(workspaceId)).stream().map(this::message).toList();
    }

    @GetMapping("/messages/{messageId}")
    ControlPlaneApi.MessageResponse getMessage(@PathVariable String workspaceId, @PathVariable String messageId) {
        return message(messages.get(new WorkspaceId(workspaceId), messageId));
    }

    @PostMapping("/audiences")
    ResponseEntity<ControlPlaneApi.AudienceResponse> createAudience(
            @PathVariable String workspaceId, @RequestBody ControlPlaneApi.CreateAudienceRequest request) {
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        List<AudienceApplicationService.RecipientInput> recipients = request.recipients() == null ? null
                : request.recipients().stream().map(item ->
                new AudienceApplicationService.RecipientInput(item.itemId(), item.fields())).toList();
        AudienceDefinition created = audiences.create(workspace,
                new AudienceApplicationService.CreateAudience(request.name(), recipients));
        return created("/api/v1/workspaces/" + workspaceId + "/audiences/" + created.id(), audience(created));
    }

    @GetMapping("/audiences")
    List<ControlPlaneApi.AudienceResponse> listAudiences(@PathVariable String workspaceId) {
        return audiences.list(new WorkspaceId(workspaceId)).stream()
                .map(ControlPlaneController::audience).toList();
    }

    @GetMapping("/audiences/{audienceId}")
    ControlPlaneApi.AudienceResponse getAudience(@PathVariable String workspaceId,
                                                 @PathVariable String audienceId) {
        return audience(audiences.get(new WorkspaceId(workspaceId), audienceId));
    }

    @PostMapping("/jobs")
    ResponseEntity<ControlPlaneApi.JobResponse> createJob(
            @PathVariable String workspaceId, @RequestBody ControlPlaneApi.CreateJobRequest request) {
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        JobDefinition created = jobs.create(workspace, new JobApplicationService.CreateJob(
                request.name(), request.accountId(), request.messageId(), request.audienceId(),
                request.policies(), request.enabled() == null || request.enabled()));
        return created("/api/v1/workspaces/" + workspaceId + "/jobs/" + created.id(), job(created));
    }

    @GetMapping("/jobs")
    List<ControlPlaneApi.JobResponse> listJobs(@PathVariable String workspaceId) {
        return jobs.list(new WorkspaceId(workspaceId)).stream().map(this::job).toList();
    }

    @GetMapping("/jobs/{jobId}")
    ControlPlaneApi.JobResponse getJob(@PathVariable String workspaceId, @PathVariable String jobId) {
        return job(jobs.get(new WorkspaceId(workspaceId), jobId));
    }

    @PostMapping("/jobs/{jobId}/runs")
    ResponseEntity<ControlPlaneApi.RunResponse> createRun(
            @PathVariable String workspaceId, @PathVariable String jobId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) ControlPlaneApi.CreateRunRequest nullableRequest) {
        ControlPlaneApi.CreateRunRequest request = nullableRequest == null
                ? new ControlPlaneApi.CreateRunRequest(false, Map.of(), "manual") : nullableRequest;
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        RunApplicationService.CreationResult result = runs.create(workspace, jobId, idempotencyKey,
                new RunApplicationService.CreateRun(Boolean.TRUE.equals(request.dryRun()),
                        request.policyOverrides(), request.reason()));
        ControlPlaneApi.RunResponse response = run(result.run());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .header(HttpHeaders.LOCATION, response.links().get("self"))
                .body(response);
    }

    @GetMapping("/runs")
    List<ControlPlaneApi.RunResponse> listRuns(@PathVariable String workspaceId) {
        return runs.list(new WorkspaceId(workspaceId)).stream()
                .map(ControlPlaneController::run).toList();
    }

    @GetMapping("/runs/{runId}")
    ControlPlaneApi.RunResponse getRun(@PathVariable String workspaceId, @PathVariable String runId) {
        return run(runs.get(new WorkspaceId(workspaceId), runId));
    }

    @GetMapping("/runs/{runId}/items")
    ControlPlaneApi.RunItemResultPage runItems(
            @PathVariable String workspaceId, @PathVariable String runId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100") int limit) {
        RunResultApplicationService.Page page = results.page(
                new WorkspaceId(workspaceId), runId, cursor, limit);
        return new ControlPlaneApi.RunItemResultPage(page.items().stream().map(this::result).toList(),
                new ControlPlaneApi.CursorPage(page.nextCursor(), page.hasMore()));
    }

    @PostMapping("/runs/{runId}/commands/pause")
    ResponseEntity<ControlPlaneApi.RunCommandResponse> pauseRun(
            @PathVariable String workspaceId, @PathVariable String runId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return command(commands.pause(new WorkspaceId(workspaceId), runId, idempotencyKey));
    }

    @PostMapping("/runs/{runId}/commands/resume")
    ResponseEntity<ControlPlaneApi.RunCommandResponse> resumeRun(
            @PathVariable String workspaceId, @PathVariable String runId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return command(commands.resume(new WorkspaceId(workspaceId), runId, idempotencyKey));
    }

    @PostMapping("/runs/{runId}/commands/cancel")
    ResponseEntity<ControlPlaneApi.RunCommandResponse> cancelRun(
            @PathVariable String workspaceId, @PathVariable String runId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ControlPlaneApi.CancelRunRequest request) {
        return command(commands.cancel(new WorkspaceId(workspaceId), runId,
                idempotencyKey, request.reason()));
    }

    @PostMapping("/runs/{runId}/commands/concurrency")
    ResponseEntity<ControlPlaneApi.RunCommandResponse> changeRunConcurrency(
            @PathVariable String workspaceId, @PathVariable String runId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ControlPlaneApi.ChangeConcurrencyRequest request) {
        return command(commands.changeConcurrency(new WorkspaceId(workspaceId), runId,
                idempotencyKey, request.target()));
    }

    private ControlPlaneApi.AccountResponse account(AccountDefinition value) {
        return new ControlPlaneApi.AccountResponse(value.id(), value.workspaceId().value(), value.name(),
                value.provider().providerId(), value.provider().implementationVersion(),
                json.read(value.configuration(), Object.class), value.status().name(), value.createdAt(),
                value.updatedAt(), value.version());
    }

    private ControlPlaneApi.MessageResponse message(MessageDefinition value) {
        return new ControlPlaneApi.MessageResponse(value.id(), value.workspaceId().value(), value.name(),
                value.provider().providerId(), value.provider().implementationVersion(), value.revision(),
                value.schemaVersion(), json.read(value.content(), Object.class), value.contentHash(),
                value.status().name(), value.createdAt(), value.updatedAt(), value.version());
    }

    private static ControlPlaneApi.AudienceResponse audience(AudienceDefinition value) {
        return new ControlPlaneApi.AudienceResponse(value.id(), value.workspaceId().value(), value.name(),
                value.snapshotId(), value.revision(), value.recordCount(), value.contentHash(),
                value.status().name(), value.createdAt(), value.updatedAt(), value.version());
    }

    private ControlPlaneApi.JobResponse job(JobDefinition value) {
        return new ControlPlaneApi.JobResponse(value.id(), value.workspaceId().value(), value.name(),
                value.accountId(), value.messageId(), value.audienceId(),
                json.read(value.policies(), Object.class), value.enabled(), value.createdAt(),
                value.updatedAt(), value.version());
    }

    private ControlPlaneApi.RunItemResultResponse result(RunItemResultRecord value) {
        return new ControlPlaneApi.RunItemResultResponse(value.runId(), value.itemId(), value.attempts(),
                value.state().name(), value.providerCode(), value.diagnostic(), value.externalRequestId(),
                value.completedAt(), json.read(value.metadata(), Object.class));
    }

    private static ControlPlaneApi.RunResponse run(RunDefinition value) {
        String base = "/api/v1/workspaces/" + value.workspaceId().value() + "/runs/" + value.id();
        return new ControlPlaneApi.RunResponse(value.id(), value.workspaceId().value(), value.jobId(),
                value.status().name(), value.stateReason(), value.dryRun(),
                new ControlPlaneApi.RunCounters(value.total(), value.succeeded(), value.failed(), value.unknown(),
                        value.unsent(), value.skipped(), value.retried()), value.createdAt(), value.startedAt(),
                value.endedAt(), value.updatedAt(), value.version(),
                Map.of("self", base, "events", base + "/events", "items", base + "/items",
                        "artifacts", base + "/artifacts"));
    }

    private static <T> ResponseEntity<T> created(String location, T body) {
        return ResponseEntity.created(URI.create(location)).body(body);
    }

    private static ResponseEntity<ControlPlaneApi.RunCommandResponse> command(
            RunCommandApplicationService.Result result) {
        ControlPlaneApi.RunCommandResponse body = new ControlPlaneApi.RunCommandResponse(
                result.commandId(), result.type(), result.status().name(), result.code(), result.message(),
                result.acknowledgedAt(), result.replayed());
        HttpStatus status = result.status() == com.fangxuele.wepush.next.service.domain.RunCommandRecord.Status.ACCEPTED
                ? (result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(body);
    }
}

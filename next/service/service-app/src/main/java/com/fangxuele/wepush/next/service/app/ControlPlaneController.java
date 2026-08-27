package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.AccountApplicationService;
import com.fangxuele.wepush.next.service.application.ControlPlaneQueryService;
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
import com.fangxuele.wepush.next.service.domain.MessageRevision;
import com.fangxuele.wepush.next.service.domain.RunOverview;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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
    private final ControlPlaneQueryService queries;
    private final JsonCodec json;

    ControlPlaneController(AccountApplicationService accounts, MessageApplicationService messages,
                           AudienceApplicationService audiences, JobApplicationService jobs,
                           RunApplicationService runs, RunResultApplicationService results,
                           RunCommandApplicationService commands, ControlPlaneQueryService queries,
                           JsonCodec json) {
        this.accounts = accounts;
        this.messages = messages;
        this.audiences = audiences;
        this.jobs = jobs;
        this.runs = runs;
        this.results = results;
        this.commands = commands;
        this.queries = queries;
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
    ControlPlaneApi.ResourcePageResponse<ControlPlaneApi.AccountResponse> listAccounts(
            @PathVariable String workspaceId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        var page = queries.accounts(new WorkspaceId(workspaceId), filters(cursor, limit, name, status, from, to));
        return resourcePage(page, this::account);
    }

    @GetMapping("/accounts/{accountId}")
    ControlPlaneApi.AccountResponse getAccount(@PathVariable String workspaceId, @PathVariable String accountId) {
        return account(accounts.get(new WorkspaceId(workspaceId), accountId));
    }

    @PatchMapping("/accounts/{accountId}")
    ControlPlaneApi.AccountResponse updateAccount(@PathVariable String workspaceId, @PathVariable String accountId,
                                                  @RequestBody ControlPlaneApi.UpdateAccountRequest request) {
        return account(accounts.update(new WorkspaceId(workspaceId), accountId,
                new AccountApplicationService.UpdateAccount(request.name(), request.configuration(), request.status())));
    }

    @PostMapping("/accounts/{accountId}/connection-test")
    ControlPlaneApi.ConnectionTestResponse testAccount(@PathVariable String workspaceId,
            @PathVariable String accountId, @RequestBody(required = false) ControlPlaneApi.ConnectionTestRequest request) {
        var result = accounts.testConnection(new WorkspaceId(workspaceId), accountId,
                request == null || request.timeout() == null ? Duration.ofSeconds(10) : Duration.parse(request.timeout()));
        return new ControlPlaneApi.ConnectionTestResponse(result.successful(), result.code(), result.diagnostic(),
                result.latency().toMillis());
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
    ControlPlaneApi.ResourcePageResponse<ControlPlaneApi.MessageResponse> listMessages(
            @PathVariable String workspaceId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        var page = queries.messages(new WorkspaceId(workspaceId), filters(cursor, limit, name, status, from, to));
        return resourcePage(page, this::message);
    }

    @GetMapping("/messages/{messageId}")
    ControlPlaneApi.MessageResponse getMessage(@PathVariable String workspaceId, @PathVariable String messageId) {
        return message(messages.get(new WorkspaceId(workspaceId), messageId));
    }

    @PatchMapping("/messages/{messageId}")
    ControlPlaneApi.MessageResponse updateMessage(@PathVariable String workspaceId, @PathVariable String messageId,
                                                  @RequestBody ControlPlaneApi.UpdateMessageRequest request) {
        return message(messages.update(new WorkspaceId(workspaceId), messageId,
                new MessageApplicationService.UpdateMessage(request.name(), request.content(), request.status())));
    }

    @PostMapping("/messages/{messageId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    ControlPlaneApi.MessageResponse copyMessage(@PathVariable String workspaceId, @PathVariable String messageId,
                                                @RequestBody ControlPlaneApi.CopyResourceRequest request) {
        return message(messages.copy(new WorkspaceId(workspaceId), messageId, request.name()));
    }

    @GetMapping("/messages/{messageId}/revisions")
    ControlPlaneApi.RevisionPage messageRevisions(@PathVariable String workspaceId, @PathVariable String messageId,
            @RequestParam(defaultValue = "0") int beforeRevision, @RequestParam(defaultValue = "25") int limit) {
        var page = messages.revisions(new WorkspaceId(workspaceId), messageId, beforeRevision, limit);
        return new ControlPlaneApi.RevisionPage(page.items().stream().map(this::revision).toList(),
                page.nextBeforeRevision(), page.hasMore());
    }

    @GetMapping("/messages/{messageId}/diff")
    ControlPlaneApi.MessageDiffResponse messageDiff(@PathVariable String workspaceId, @PathVariable String messageId,
                                                    @RequestParam int from, @RequestParam int to) {
        var diff = messages.diff(new WorkspaceId(workspaceId), messageId, from, to);
        return new ControlPlaneApi.MessageDiffResponse(revision(diff.from()), revision(diff.to()), diff.changedPaths());
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
    ControlPlaneApi.ResourcePageResponse<ControlPlaneApi.AudienceResponse> listAudiences(
            @PathVariable String workspaceId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        var page = queries.audiences(new WorkspaceId(workspaceId), filters(cursor, limit, name, status, from, to));
        return resourcePage(page, ControlPlaneController::audience);
    }

    @GetMapping("/audiences/{audienceId}")
    ControlPlaneApi.AudienceResponse getAudience(@PathVariable String workspaceId,
                                                 @PathVariable String audienceId) {
        return audience(audiences.get(new WorkspaceId(workspaceId), audienceId));
    }

    @PatchMapping("/audiences/{audienceId}")
    ControlPlaneApi.AudienceResponse updateAudience(@PathVariable String workspaceId,
            @PathVariable String audienceId, @RequestBody ControlPlaneApi.UpdateAudienceRequest request) {
        List<AudienceApplicationService.RecipientInput> recipients = request.recipients() == null ? null
                : request.recipients().stream().map(item ->
                new AudienceApplicationService.RecipientInput(item.itemId(), item.fields())).toList();
        return audience(audiences.update(new WorkspaceId(workspaceId), audienceId,
                new AudienceApplicationService.UpdateAudience(request.name(), recipients, request.status())));
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
    ControlPlaneApi.ResourcePageResponse<ControlPlaneApi.JobResponse> listJobs(
            @PathVariable String workspaceId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        var page = queries.jobs(new WorkspaceId(workspaceId), filters(cursor, limit, name, status, from, to));
        return resourcePage(page, this::job);
    }

    @GetMapping("/jobs/{jobId}")
    ControlPlaneApi.JobResponse getJob(@PathVariable String workspaceId, @PathVariable String jobId) {
        return job(jobs.get(new WorkspaceId(workspaceId), jobId));
    }

    @PatchMapping("/jobs/{jobId}")
    ControlPlaneApi.JobResponse updateJob(@PathVariable String workspaceId, @PathVariable String jobId,
                                          @RequestBody ControlPlaneApi.UpdateJobRequest request) {
        return job(jobs.update(new WorkspaceId(workspaceId), jobId, new JobApplicationService.UpdateJob(
                request.name(), request.accountId(), request.messageId(), request.audienceId(), request.policies(),
                request.enabled(), request.archived())));
    }

    @PostMapping("/jobs/{jobId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    ControlPlaneApi.JobResponse copyJob(@PathVariable String workspaceId, @PathVariable String jobId,
                                        @RequestBody ControlPlaneApi.CopyResourceRequest request) {
        return job(jobs.copy(new WorkspaceId(workspaceId), jobId, request.name()));
    }

    @PostMapping("/jobs/{jobId}/run-confirmation")
    ControlPlaneApi.LiveConfirmationResponse confirmRun(@PathVariable String workspaceId,
                                                        @PathVariable String jobId) {
        var value = runs.confirm(new WorkspaceId(workspaceId), jobId);
        return new ControlPlaneApi.LiveConfirmationResponse(value.jobId(), value.jobName(), value.providerId(),
                value.providerVersion(), value.accountId(), value.accountName(), value.audienceId(),
                value.audienceName(), value.audienceCount(), value.policies(), value.targetConcurrency(),
                value.rateLimitPermits(), value.rateLimitPeriod(), value.estimatedItems(), value.expiresAt(),
                value.confirmationToken());
    }

    @PostMapping("/jobs/{jobId}/runs")
    ResponseEntity<ControlPlaneApi.RunResponse> createRun(
            @PathVariable String workspaceId, @PathVariable String jobId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) ControlPlaneApi.CreateRunRequest nullableRequest) {
        ControlPlaneApi.CreateRunRequest request = nullableRequest == null
                ? new ControlPlaneApi.CreateRunRequest(false, Map.of(), "manual", null) : nullableRequest;
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        RunApplicationService.CreationResult result = runs.create(workspace, jobId, idempotencyKey,
                new RunApplicationService.CreateRun(Boolean.TRUE.equals(request.dryRun()),
                        request.policyOverrides(), request.reason(), request.confirmationToken()));
        ControlPlaneApi.RunResponse response = run(result.run());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .header(HttpHeaders.LOCATION, response.links().get("self"))
                .body(response);
    }

    @GetMapping("/runs")
    ControlPlaneApi.ResourcePageResponse<ControlPlaneApi.RunResponse> listRuns(
            @PathVariable String workspaceId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        var page = queries.runs(new WorkspaceId(workspaceId), filters(cursor, limit, name, status, from, to));
        return resourcePage(page, this::run);
    }

    @GetMapping("/runs/{runId}")
    ControlPlaneApi.RunResponse getRun(@PathVariable String workspaceId, @PathVariable String runId) {
        return run(runs.get(new WorkspaceId(workspaceId), runId));
    }

    @PostMapping("/runs/{runId}/retry-confirmation")
    ControlPlaneApi.RetryConfirmationResponse confirmRetry(@PathVariable String workspaceId,
            @PathVariable String runId, @RequestBody ControlPlaneApi.RetryRequest request) {
        var value = runs.confirmRetry(new WorkspaceId(workspaceId), runId, request.states());
        return new ControlPlaneApi.RetryConfirmationResponse(value.sourceRunId(), value.states(),
                value.itemCount(), value.expiresAt(), value.confirmationToken());
    }

    @PostMapping("/runs/{runId}/retries")
    ResponseEntity<ControlPlaneApi.RunResponse> retryRun(@PathVariable String workspaceId,
            @PathVariable String runId, @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ControlPlaneApi.RetryRequest request) {
        var result = runs.retry(new WorkspaceId(workspaceId), runId, idempotencyKey,
                new RunApplicationService.RetryRun(request.states(), request.confirmationToken()));
        var response = run(result.run());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .header(HttpHeaders.LOCATION, response.links().get("self")).body(response);
    }

    @GetMapping("/overview")
    ControlPlaneApi.OverviewResponse overview(@PathVariable String workspaceId) {
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        RunOverview summary = queries.overview(workspace);
        var recentPage = queries.runs(workspace, new ControlPlaneQueryService.Filters(
                null, 10, null, null, null, null));
        return new ControlPlaneApi.OverviewResponse(summary.activeRuns(), summary.totalRuns(),
                summary.succeededRuns(), summary.problemRuns(), queries.activeRuns(workspace, 10).stream()
                        .map(this::run).toList(),
                recentPage.items().stream().map(this::run).toList(), summary.trend().stream().map(point ->
                new ControlPlaneApi.RunTrendResponse(point.day().toString(), point.total(), point.succeeded(),
                        point.problem())).toList());
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

    private ControlPlaneApi.MessageRevisionResponse revision(MessageRevision value) {
        return new ControlPlaneApi.MessageRevisionResponse(value.messageId(), value.revision(),
                value.schemaVersion(), json.read(value.content(), Object.class), value.contentHash(),
                value.createdAt());
    }

    private static ControlPlaneApi.AudienceResponse audience(AudienceDefinition value) {
        return new ControlPlaneApi.AudienceResponse(value.id(), value.workspaceId().value(), value.name(),
                value.snapshotId(), value.revision(), value.recordCount(), value.contentHash(),
                value.status().name(), value.createdAt(), value.updatedAt(), value.version());
    }

    private ControlPlaneApi.JobResponse job(JobDefinition value) {
        return new ControlPlaneApi.JobResponse(value.id(), value.workspaceId().value(), value.name(),
                value.accountId(), value.messageId(), value.audienceId(),
                json.read(value.policies(), Object.class), value.enabled(), value.archived(), value.status(),
                value.createdAt(),
                value.updatedAt(), value.version());
    }

    private ControlPlaneApi.RunItemResultResponse result(RunItemResultRecord value) {
        return new ControlPlaneApi.RunItemResultResponse(value.runId(), value.itemId(), value.attempts(),
                value.state().name(), value.providerCode(), value.diagnostic(), value.externalRequestId(),
                value.completedAt(), json.read(value.metadata(), Object.class));
    }

    private ControlPlaneApi.RunResponse run(RunDefinition value) {
        String base = "/api/v1/workspaces/" + value.workspaceId().value() + "/runs/" + value.id();
        String jobName;
        try { jobName = jobs.get(value.workspaceId(), value.jobId()).name(); }
        catch (RuntimeException missing) { jobName = value.jobId(); }
        return new ControlPlaneApi.RunResponse(value.id(), value.workspaceId().value(), value.jobId(), jobName,
                value.status().name(), value.stateReason(), value.dryRun(),
                new ControlPlaneApi.RunCounters(value.total(), value.succeeded(), value.failed(), value.unknown(),
                        value.unsent(), value.skipped(), value.retried()), value.sourceRunId(), value.retryStates(),
                value.createdAt(), value.startedAt(),
                value.endedAt(), value.updatedAt(), value.version(),
                Map.of("self", base, "events", base + "/events", "items", base + "/items",
                        "artifacts", base + "/artifacts"));
    }

    private static ControlPlaneQueryService.Filters filters(String cursor, int limit, String name,
                                                            String status, Instant from, Instant to) {
        return new ControlPlaneQueryService.Filters(cursor, limit, name, status, from, to);
    }

    private static <S, T> ControlPlaneApi.ResourcePageResponse<T> resourcePage(
            ControlPlaneQueryService.Page<S> page, java.util.function.Function<S, T> mapper) {
        return new ControlPlaneApi.ResourcePageResponse<>(page.items().stream().map(mapper).toList(),
                new ControlPlaneApi.CursorPage(page.nextCursor(), page.hasMore()));
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

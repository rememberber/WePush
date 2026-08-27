package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class JobApplicationService {
    private final WorkspaceRepository workspaces;
    private final AccountRepository accounts;
    private final MessageRepository messages;
    private final AudienceRepository audiences;
    private final JobRepository jobs;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public JobApplicationService(WorkspaceRepository workspaces, AccountRepository accounts,
                                 MessageRepository messages, AudienceRepository audiences,
                                 JobRepository jobs, JsonCodec json, ResourceIdGenerator ids,
                                 TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.accounts = accounts;
        this.messages = messages;
        this.audiences = audiences;
        this.jobs = jobs;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public JobDefinition create(WorkspaceId workspaceId, CreateJob command) {
        return transactions.required(() -> {
            ApplicationSupport.requireWorkspace(workspaces, workspaceId);
            AccountDefinition account = accounts.findById(workspaceId, command.accountId())
                    .orElseThrow(() -> missing("ACCOUNT_NOT_FOUND", "Account", command.accountId()));
            MessageDefinition message = messages.findById(workspaceId, command.messageId())
                    .orElseThrow(() -> missing("MESSAGE_NOT_FOUND", "Message", command.messageId()));
            AudienceDefinition audience = audiences.findById(workspaceId, command.audienceId())
                    .orElseThrow(() -> missing("AUDIENCE_NOT_FOUND", "Audience", command.audienceId()));
            if (!account.provider().equals(message.provider())) {
                throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "PROVIDER_MISMATCH",
                        "Account and message must use the same Provider version");
            }
            if (account.status() != AccountDefinition.Status.ACTIVE
                    || message.status() != MessageDefinition.Status.ACTIVE
                    || audience.status() != AudienceDefinition.Status.ACTIVE) {
                throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "RESOURCE_INACTIVE",
                        "Job resources must all be active");
            }
            JsonDocument policies = json.canonicalize(command.policies() == null ? Map.of() : command.policies());
            json.read(policies, Map.class);
            Instant now = clock.instant();
            JobDefinition job = new JobDefinition(ids.next("job"), workspaceId,
                    ApplicationSupport.text(command.name(), "name"), account.id(), message.id(), audience.id(),
                    policies, command.enabled(), false, now, now, 0);
            jobs.create(job);
            return job;
        });
    }

    public JobDefinition get(WorkspaceId workspaceId, String jobId) {
        return jobs.findById(workspaceId, jobId).orElseThrow(() ->
                missing("JOB_NOT_FOUND", "Job", jobId));
    }

    public List<JobDefinition> list(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return jobs.list(workspaceId);
    }

    public JobDefinition update(WorkspaceId workspaceId, String jobId, UpdateJob command) {
        return transactions.required(() -> {
            JobDefinition current = get(workspaceId, jobId);
            String accountId = command.accountId() == null ? current.accountId() : command.accountId();
            String messageId = command.messageId() == null ? current.messageId() : command.messageId();
            String audienceId = command.audienceId() == null ? current.audienceId() : command.audienceId();
            validateResources(workspaceId, accountId, messageId, audienceId);
            boolean archived = command.archived() == null ? current.archived() : command.archived();
            boolean enabled = command.enabled() == null ? current.enabled() : command.enabled();
            if (archived) enabled = false;
            JsonDocument policies = command.policies() == null ? current.policies()
                    : json.canonicalize(command.policies());
            json.read(policies, Map.class);
            JobDefinition updated = new JobDefinition(current.id(), workspaceId,
                    command.name() == null ? current.name() : ApplicationSupport.text(command.name(), "name"),
                    accountId, messageId, audienceId, policies, enabled, archived,
                    current.createdAt(), clock.instant(), current.version() + 1);
            if (!jobs.update(updated, current.version())) conflict(jobId);
            return updated;
        });
    }

    public JobDefinition copy(WorkspaceId workspaceId, String jobId, String name) {
        JobDefinition source = get(workspaceId, jobId);
        return create(workspaceId, new CreateJob(ApplicationSupport.text(name, "name"), source.accountId(),
                source.messageId(), source.audienceId(), json.read(source.policies(), Object.class), false));
    }

    private void validateResources(WorkspaceId workspaceId, String accountId,
                                   String messageId, String audienceId) {
        AccountDefinition account = accounts.findById(workspaceId, accountId)
                .orElseThrow(() -> missing("ACCOUNT_NOT_FOUND", "Account", accountId));
        MessageDefinition message = messages.findById(workspaceId, messageId)
                .orElseThrow(() -> missing("MESSAGE_NOT_FOUND", "Message", messageId));
        AudienceDefinition audience = audiences.findById(workspaceId, audienceId)
                .orElseThrow(() -> missing("AUDIENCE_NOT_FOUND", "Audience", audienceId));
        if (!account.provider().equals(message.provider())) {
            throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "PROVIDER_MISMATCH",
                    "Account and message must use the same Provider version");
        }
        if (account.status() != AccountDefinition.Status.ACTIVE
                || message.status() != MessageDefinition.Status.ACTIVE
                || audience.status() != AudienceDefinition.Status.ACTIVE) {
            throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "RESOURCE_INACTIVE",
                    "Job resources must all be active");
        }
    }

    private static void conflict(String id) {
        throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RESOURCE_VERSION_CONFLICT",
                "Job was changed concurrently: " + id);
    }

    private static ApplicationProblem missing(String code, String type, String id) {
        return new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, code,
                type + " was not found: " + id);
    }

    public record CreateJob(String name, String accountId, String messageId, String audienceId,
                            Object policies, boolean enabled) {
    }

    public record UpdateJob(String name, String accountId, String messageId, String audienceId,
                            Object policies, Boolean enabled, Boolean archived) { }
}

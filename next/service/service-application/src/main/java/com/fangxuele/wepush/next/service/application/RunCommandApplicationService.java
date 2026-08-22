package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.RunCommandRecord;
import com.fangxuele.wepush.next.service.domain.RunCommandRepository;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunStatus;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class RunCommandApplicationService {
    private final RunRepository runs;
    private final RunCommandRepository commands;
    private final RunCommandGateway gateway;
    private final JsonCodec json;
    private final TransactionRunner transactions;
    private final RunEventPublisher events;
    private final Clock clock;

    public RunCommandApplicationService(RunRepository runs, RunCommandRepository commands,
                                        RunCommandGateway gateway, JsonCodec json,
                                        TransactionRunner transactions, RunEventPublisher events, Clock clock) {
        this.runs = runs;
        this.commands = commands;
        this.gateway = gateway;
        this.json = json;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    public Result pause(WorkspaceId workspaceId, String runId, String idempotencyKey) {
        return submit(workspaceId, runId, idempotencyKey, "PAUSE", Map.of(),
                id -> new RunCommand.PauseRun(id));
    }

    public Result resume(WorkspaceId workspaceId, String runId, String idempotencyKey) {
        return submit(workspaceId, runId, idempotencyKey, "RESUME", Map.of(),
                id -> new RunCommand.ResumeRun(id));
    }

    public Result cancel(WorkspaceId workspaceId, String runId, String idempotencyKey, String reason) {
        String normalized = ApplicationSupport.text(reason, "reason");
        return submit(workspaceId, runId, idempotencyKey, "CANCEL", Map.of("reason", normalized),
                id -> new RunCommand.CancelRun(id, normalized));
    }

    public Result changeConcurrency(WorkspaceId workspaceId, String runId,
                                    String idempotencyKey, int target) {
        if (target < 1) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_CONCURRENCY",
                    "Concurrency target must be positive");
        }
        return submit(workspaceId, runId, idempotencyKey, "CONCURRENCY", Map.of("target", target),
                id -> new RunCommand.ChangeConcurrency(id, target));
    }

    private Result submit(WorkspaceId workspaceId, String runId, String idempotencyKey,
                          String type, Object payloadValue, CommandFactory factory) {
        String key = ApplicationSupport.text(idempotencyKey, "Idempotency-Key");
        RunDefinition run = runs.findById(workspaceId, runId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "RUN_NOT_FOUND",
                        "Run was not found: " + runId));
        String commandId = "cmd_" + ApplicationSupport.sha256(
                workspaceId.value() + "\u0000" + runId + "\u0000" + key).substring(0, 32);
        JsonDocument payload = json.canonicalize(payloadValue);
        RunCommandRecord existing = commands.findById(workspaceId, runId, commandId).orElse(null);
        if (existing != null) {
            requireSameRequest(existing, type, payload);
            if (existing.status() != RunCommandRecord.Status.PENDING) {
                return result(existing, true);
            }
        } else {
            RunCommandRecord pending = new RunCommandRecord(commandId, workspaceId, runId, type, payload,
                    RunCommandRecord.Status.PENDING, "", "", clock.instant(), null);
            boolean created = transactions.required(() -> commands.create(pending));
            if (!created) {
                existing = commands.findById(workspaceId, runId, commandId)
                        .orElseThrow(() -> new IllegalStateException("command was concurrently created"));
                requireSameRequest(existing, type, payload);
                if (existing.status() != RunCommandRecord.Status.PENDING) {
                    return result(existing, true);
                }
            }
        }

        CommandResult coreResult = gateway.submit(workspaceId, runId, factory.create(commandId));
        Instant acknowledgedAt = clock.instant();
        RunCommandRecord.Status status = coreResult.status() == CommandResult.Status.ACCEPTED
                ? RunCommandRecord.Status.ACCEPTED : RunCommandRecord.Status.REJECTED;
        RunEventRecord event = transactions.required(() -> {
            commands.acknowledge(workspaceId, runId, commandId, status,
                    coreResult.code(), coreResult.message(), acknowledgedAt);
            if (status == RunCommandRecord.Status.ACCEPTED) {
                applyAcceptedTransition(workspaceId, run, type, acknowledgedAt);
            }
            RunEventRecord recorded = new RunEventRecord(runId, workspaceId,
                    runs.nextEventSequence(workspaceId, runId), "RUN_COMMAND_" + status.name(),
                    acknowledgedAt, json.canonicalize(Map.of(
                    "commandId", commandId, "type", type, "code", coreResult.code())),
                    status == RunCommandRecord.Status.ACCEPTED
                            ? RunEventRecord.Severity.INFO : RunEventRecord.Severity.WARNING);
            runs.appendEvent(recorded);
            return recorded;
        });
        events.publish(event);
        return new Result(commandId, type, status, coreResult.code(), coreResult.message(),
                acknowledgedAt, false);
    }

    private void applyAcceptedTransition(WorkspaceId workspaceId, RunDefinition run,
                                         String type, Instant changedAt) {
        switch (type) {
            case "PAUSE" -> runs.transition(workspaceId, run.id(), Set.of(RunStatus.RUNNING),
                    RunStatus.PAUSED, "paused by command", changedAt);
            case "RESUME" -> runs.transition(workspaceId, run.id(), Set.of(RunStatus.PAUSED),
                    RunStatus.RUNNING, "resumed by command", changedAt);
            case "CANCEL" -> runs.transition(workspaceId, run.id(),
                    Set.of(RunStatus.PENDING, RunStatus.RUNNING, RunStatus.PAUSED, RunStatus.RECOVERING),
                    RunStatus.CANCELLING, "cancel requested", changedAt);
            case "CONCURRENCY" -> {
                // Core owns the live concurrency gate; the command/event remain the durable audit trail.
            }
            default -> throw new IllegalArgumentException("unsupported command type: " + type);
        }
    }

    private static void requireSameRequest(RunCommandRecord existing, String type, JsonDocument payload) {
        if (!existing.type().equals(type) || !existing.payload().equals(payload)) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was already used for a different Run command");
        }
    }

    private static Result result(RunCommandRecord record, boolean replayed) {
        return new Result(record.id(), record.type(), record.status(), record.resultCode(),
                record.resultMessage(), record.acknowledgedAt(), replayed);
    }

    public record Result(String commandId, String type, RunCommandRecord.Status status,
                         String code, String message, Instant acknowledgedAt, boolean replayed) {
    }

    @FunctionalInterface
    private interface CommandFactory {
        RunCommand create(String commandId);
    }
}

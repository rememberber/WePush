package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicy;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicyRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;

/** Performs serialized, fail-closed Workspace capacity checks. */
public final class WorkspaceResourceGovernor {
    private final WorkspaceRepository workspaces;
    private final WorkspacePolicyRepository policies;
    private final TransactionRunner transactions;
    private final Clock clock;

    public WorkspaceResourceGovernor(WorkspaceRepository workspaces, WorkspacePolicyRepository policies,
                                     TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.policies = policies;
        this.transactions = transactions;
        this.clock = clock;
    }

    public WorkspacePolicy policy(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return policies.find(workspaceId).orElseThrow(() -> new IllegalStateException("Workspace Policy is missing"));
    }

    public WorkspacePolicyRepository.Usage usage(WorkspaceId workspaceId) {
        policy(workspaceId);
        return policies.usage(workspaceId);
    }

    public WorkspacePolicy update(WorkspaceId workspaceId, Limits limits) {
        return transactions.required(() -> {
            WorkspacePolicy current = policy(workspaceId);
            WorkspacePolicy updated = new WorkspacePolicy(workspaceId,
                    limits.maxAgents(), limits.maxConcurrentRuns(), limits.maxTotalConcurrency(),
                    limits.artifactQuotaBytes(), limits.artifactRetentionSeconds(), clock.instant(),
                    current.version());
            policies.save(updated, current.version());
            return policies.find(workspaceId).orElseThrow();
        });
    }

    public void requireAgentCapacity(WorkspaceId workspaceId, String agentId) {
        policies.lock(workspaceId);
        WorkspacePolicy policy = required(workspaceId);
        if (policies.agentAlreadyBound(workspaceId, agentId)) return;
        long used = policies.usage(workspaceId).agents();
        require(policy.maxAgents() == 0 || used < policy.maxAgents(), "WORKSPACE_AGENT_LIMIT",
                "Workspace Agent limit is reached", used, policy.maxAgents());
    }

    public void reserveRun(WorkspaceId workspaceId, String runId, int targetConcurrency) {
        if (targetConcurrency < 1) throw new IllegalArgumentException("target concurrency must be positive");
        policies.lock(workspaceId);
        WorkspacePolicy policy = required(workspaceId);
        WorkspacePolicyRepository.Usage usage = policies.usage(workspaceId);
        require(policy.maxConcurrentRuns() == 0 || usage.concurrentRuns() < policy.maxConcurrentRuns(),
                "WORKSPACE_RUN_LIMIT", "Workspace concurrent Run limit is reached",
                usage.concurrentRuns(), policy.maxConcurrentRuns());
        require(policy.maxTotalConcurrency() == 0
                        || usage.totalConcurrency() + targetConcurrency <= policy.maxTotalConcurrency(),
                "WORKSPACE_CONCURRENCY_LIMIT", "Workspace aggregate concurrency limit would be exceeded",
                usage.totalConcurrency() + targetConcurrency, policy.maxTotalConcurrency());
        policies.reserveRun(workspaceId, runId, targetConcurrency, clock.instant());
    }

    public void requireArtifactCapacity(WorkspaceId workspaceId, long additionalBytes) {
        if (additionalBytes < 0) throw new IllegalArgumentException("additional Artifact bytes must not be negative");
        policies.lock(workspaceId);
        WorkspacePolicy policy = required(workspaceId);
        long used = policies.usage(workspaceId).artifactBytes();
        require(policy.artifactQuotaBytes() == 0
                        || additionalBytes <= policy.artifactQuotaBytes() - Math.min(used, policy.artifactQuotaBytes()),
                "WORKSPACE_ARTIFACT_QUOTA", "Workspace Artifact quota would be exceeded",
                saturatedAdd(used, additionalBytes), policy.artifactQuotaBytes());
    }

    private WorkspacePolicy required(WorkspaceId workspaceId) {
        return policies.find(workspaceId).orElseThrow(() -> new IllegalStateException("Workspace Policy is missing"));
    }

    private static void require(boolean accepted, String code, String message, long requested, long limit) {
        if (!accepted) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, code,
                    message + " (requested/used=" + requested + ", limit=" + limit + ")");
        }
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    public record Limits(int maxAgents, int maxConcurrentRuns, int maxTotalConcurrency,
                         long artifactQuotaBytes, long artifactRetentionSeconds) {
        public Limits {
            // Reuse the domain validation and keep the API command immutable.
            new WorkspacePolicy(new WorkspaceId("validation"), maxAgents, maxConcurrentRuns,
                    maxTotalConcurrency, artifactQuotaBytes, artifactRetentionSeconds,
                    java.time.Instant.EPOCH, 0);
        }
    }
}

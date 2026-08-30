package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.Workspace;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicy;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicyRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WorkspaceResourceGovernorTest {
    private static final Instant NOW = Instant.parse("2026-08-29T00:00:00Z");
    private static final WorkspaceId WORKSPACE = new WorkspaceId("ws_test");

    @Test
    void enforcesAgentRunConcurrencyAndArtifactLimits() {
        FakePolicies policies = new FakePolicies();
        WorkspaceResourceGovernor governor = new WorkspaceResourceGovernor(new FakeWorkspaces(), policies,
                new DirectTransactions(), Clock.fixed(NOW, ZoneOffset.UTC));

        policies.usage = new WorkspacePolicyRepository.Usage(1, 0, 0, 0);
        assertCode("WORKSPACE_AGENT_LIMIT",
                () -> governor.requireAgentCapacity(WORKSPACE, "new-agent"));
        governor.requireAgentCapacity(WORKSPACE, "bound-agent");

        policies.usage = new WorkspacePolicyRepository.Usage(1, 1, 2, 100);
        assertCode("WORKSPACE_RUN_LIMIT", () -> governor.reserveRun(WORKSPACE, "run-2", 1));
        policies.usage = new WorkspacePolicyRepository.Usage(1, 0, 3, 100);
        assertCode("WORKSPACE_CONCURRENCY_LIMIT", () -> governor.reserveRun(WORKSPACE, "run-2", 2));
        governor.reserveRun(WORKSPACE, "run-2", 1);
        assertEquals("run-2", policies.reservedRun);

        policies.usage = new WorkspacePolicyRepository.Usage(1, 0, 0, 900);
        assertCode("WORKSPACE_ARTIFACT_QUOTA", () -> governor.requireArtifactCapacity(WORKSPACE, 101));
        governor.requireArtifactCapacity(WORKSPACE, 100);
    }

    private static void assertCode(String code, Runnable work) {
        ApplicationProblem problem = assertThrows(ApplicationProblem.class, work::run);
        assertEquals(code, problem.code());
    }

    private static final class FakePolicies implements WorkspacePolicyRepository {
        private WorkspacePolicy policy = new WorkspacePolicy(WORKSPACE, 1, 1, 4, 1000, 3600, NOW, 0);
        private Usage usage = new Usage(0, 0, 0, 0);
        private String reservedRun;

        @Override public Optional<WorkspacePolicy> find(WorkspaceId workspaceId) { return Optional.of(policy); }
        @Override public void createDefault(WorkspaceId workspaceId, Instant now) { }
        @Override public void save(WorkspacePolicy value, long expectedVersion) { policy = value; }
        @Override public void lock(WorkspaceId workspaceId) { }
        @Override public Usage usage(WorkspaceId workspaceId) { return usage; }
        @Override public boolean agentAlreadyBound(WorkspaceId workspaceId, String agentId) {
            return "bound-agent".equals(agentId);
        }
        @Override public void reserveRun(WorkspaceId workspaceId, String runId, int targetConcurrency,
                                         Instant createdAt) { reservedRun = runId; }
    }

    private static final class FakeWorkspaces implements WorkspaceRepository {
        @Override public Optional<Workspace> findById(WorkspaceId workspaceId) {
            return Optional.of(new Workspace(WORKSPACE, "Test", Workspace.Status.ACTIVE, NOW, 0));
        }
        @Override public List<Workspace> list() { return List.of(); }
        @Override public void create(Workspace workspace) { }
        @Override public void save(Workspace workspace, long expectedVersion) { }
    }

    private static final class DirectTransactions implements TransactionRunner {
        @Override public <T> T required(Supplier<T> work) { return work.get(); }
    }
}

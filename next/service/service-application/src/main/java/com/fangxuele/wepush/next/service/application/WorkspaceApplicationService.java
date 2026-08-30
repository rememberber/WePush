package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.Workspace;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicyRepository;

import java.time.Clock;
import java.util.List;

public final class WorkspaceApplicationService {
    private final WorkspaceRepository workspaces;
    private final WorkspacePolicyRepository policies;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public WorkspaceApplicationService(WorkspaceRepository workspaces, WorkspacePolicyRepository policies,
                                       ResourceIdGenerator ids,
                                       TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.policies = policies;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public List<Workspace> list() {
        return workspaces.list();
    }

    public Workspace get(WorkspaceId id) {
        return workspaces.findById(id).orElseThrow(() -> new ApplicationProblem(
                ApplicationProblem.Kind.NOT_FOUND, "WORKSPACE_NOT_FOUND",
                "Workspace was not found: " + id.value()));
    }

    public Workspace create(String name) {
        Workspace created = new Workspace(new WorkspaceId(ids.next("ws")),
                ApplicationSupport.text(name, "name"), Workspace.Status.ACTIVE, clock.instant(), 0);
        transactions.required(() -> {
            workspaces.create(created);
            policies.createDefault(created.id(), created.createdAt());
        });
        return created;
    }
}

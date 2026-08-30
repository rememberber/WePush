package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.WorkspaceApplicationService;
import com.fangxuele.wepush.next.service.application.WorkspaceResourceGovernor;
import com.fangxuele.wepush.next.service.domain.Workspace;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicy;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
final class WorkspaceController {
    private final WorkspaceApplicationService workspaces;
    private final WorkspaceResourceGovernor resources;

    WorkspaceController(WorkspaceApplicationService workspaces, WorkspaceResourceGovernor resources) {
        this.workspaces = workspaces;
        this.resources = resources;
    }

    @GetMapping("/api/v1/workspaces")
    List<Response> list() {
        return workspaces.list().stream().map(WorkspaceController::response).toList();
    }

    @PostMapping("/api/v1/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    Response create(@RequestBody CreateRequest request) {
        return response(workspaces.create(request.name()));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}")
    Response get(@PathVariable String workspaceId) {
        return response(workspaces.get(new WorkspaceId(workspaceId)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/policy")
    PolicyResponse policy(@PathVariable String workspaceId) {
        WorkspaceId id = new WorkspaceId(workspaceId);
        return policy(resources.policy(id), resources.usage(id));
    }

    @PutMapping("/api/v1/workspaces/{workspaceId}/policy")
    PolicyResponse updatePolicy(@PathVariable String workspaceId, @RequestBody PolicyRequest request) {
        WorkspaceId id = new WorkspaceId(workspaceId);
        WorkspacePolicy updated = resources.update(id, new WorkspaceResourceGovernor.Limits(
                request.maxAgents(), request.maxConcurrentRuns(), request.maxTotalConcurrency(),
                request.artifactQuotaBytes(), request.artifactRetentionSeconds()));
        return policy(updated, resources.usage(id));
    }

    private static Response response(Workspace value) {
        return new Response(value.id().value(), value.name(), value.status().name(),
                value.createdAt(), value.version());
    }

    private static PolicyResponse policy(WorkspacePolicy value, WorkspacePolicyRepository.Usage usage) {
        return new PolicyResponse(value.workspaceId().value(), value.maxAgents(), value.maxConcurrentRuns(),
                value.maxTotalConcurrency(), value.artifactQuotaBytes(), value.artifactRetentionSeconds(),
                usage.agents(), usage.concurrentRuns(), usage.totalConcurrency(), usage.artifactBytes(),
                value.updatedAt(), value.version());
    }

    record CreateRequest(String name) { }
    record Response(String id, String name, String status, Instant createdAt, long version) { }
    record PolicyRequest(int maxAgents, int maxConcurrentRuns, int maxTotalConcurrency,
                         long artifactQuotaBytes, long artifactRetentionSeconds) { }
    record PolicyResponse(String workspaceId, int maxAgents, int maxConcurrentRuns,
                          int maxTotalConcurrency, long artifactQuotaBytes,
                          long artifactRetentionSeconds, long usedAgents, long activeRuns,
                          long usedConcurrency, long usedArtifactBytes, Instant updatedAt, long version) { }
}

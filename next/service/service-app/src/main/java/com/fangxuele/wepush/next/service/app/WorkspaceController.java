package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.WorkspaceApplicationService;
import com.fangxuele.wepush.next.service.domain.Workspace;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
final class WorkspaceController {
    private final WorkspaceApplicationService workspaces;

    WorkspaceController(WorkspaceApplicationService workspaces) {
        this.workspaces = workspaces;
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

    private static Response response(Workspace value) {
        return new Response(value.id().value(), value.name(), value.status().name(),
                value.createdAt(), value.version());
    }

    record CreateRequest(String name) { }
    record Response(String id, String name, String status, Instant createdAt, long version) { }
}

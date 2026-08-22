package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.SecretApplicationService;
import com.fangxuele.wepush.next.service.application.SecretMetadata;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/secrets/{namespace}/{name}/versions/{version}")
final class SecretController {
    private final SecretApplicationService secrets;

    SecretController(SecretApplicationService secrets) {
        this.secrets = secrets;
    }

    @PutMapping
    ControlPlaneApi.SecretMetadataResponse replace(@PathVariable String workspaceId,
                                                   @PathVariable String namespace,
                                                   @PathVariable String name,
                                                   @PathVariable String version,
                                                   @RequestBody ControlPlaneApi.SecretWriteRequest request) {
        return response(secrets.replace(new WorkspaceId(workspaceId),
                new SecretRef(namespace, name, version), request.value()));
    }

    @GetMapping
    ControlPlaneApi.SecretMetadataResponse metadata(@PathVariable String workspaceId,
                                                    @PathVariable String namespace,
                                                    @PathVariable String name,
                                                    @PathVariable String version) {
        return response(secrets.metadata(new WorkspaceId(workspaceId), new SecretRef(namespace, name, version)));
    }

    private static ControlPlaneApi.SecretMetadataResponse response(SecretMetadata value) {
        return new ControlPlaneApi.SecretMetadataResponse(value.workspaceId().value(),
                value.ref().namespace(), value.ref().name(), value.ref().version(), value.configured(),
                value.recordVersion(), value.createdAt(), value.updatedAt());
    }
}

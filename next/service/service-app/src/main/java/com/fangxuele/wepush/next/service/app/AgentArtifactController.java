package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.service.application.AgentArtifactApplicationService;
import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.application.ArtifactUploadTokenCodec;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;

@RestController
final class AgentArtifactController {
    private final AgentArtifactApplicationService artifacts;
    private final AgentHttpAuthenticator authenticator;

    AgentArtifactController(AgentArtifactApplicationService artifacts,
                            AgentHttpAuthenticator authenticator) {
        this.artifacts = artifacts;
        this.authenticator = authenticator;
    }

    @PostMapping("/internal/agent/v1/leases/{leaseId}/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    AgentArtifactApplicationService.UploadPlan create(
            @PathVariable String leaseId, @RequestBody CreateArtifactRequest request,
            @RequestHeader(value = "x-wepush-agent-token", required = false) String supplied,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticator.requireForLease(leaseId, supplied, authorization);
        return artifacts.create(new LeaseFence(leaseId, request.runId(), request.epoch(),
                        request.fencingToken()), request.type(), request.originalName(), request.contentType(),
                request.size(), request.sha256());
    }

    @PutMapping("/internal/agent/v1/artifacts/{artifactId}/content")
    UploadResult upload(@PathVariable String artifactId,
                        @RequestParam("upload_token") String uploadToken,
                        HttpServletRequest request) throws IOException {
        try {
            ArtifactStore.StoredObject stored = artifacts.upload(artifactId, uploadToken,
                    request.getInputStream());
            return new UploadResult(stored.size(), stored.sha256());
        } catch (ArtifactUploadTokenCodec.InvalidUploadTokenException problem) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, problem.getMessage(), problem);
        }
    }

    @PostMapping("/internal/agent/v1/artifacts/{artifactId}/commit")
    CommitResult commit(@PathVariable String artifactId,
                        @RequestParam("upload_token") String uploadToken) {
        try {
            var committed = artifacts.commit(artifactId, uploadToken);
            return new CommitResult(committed.id(), committed.size(), committed.sha256(),
                    committed.state().name(), committed.readyAt());
        } catch (ArtifactUploadTokenCodec.InvalidUploadTokenException problem) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, problem.getMessage(), problem);
        }
    }

    record CreateArtifactRequest(String runId, long epoch, String fencingToken, String type,
                                 String originalName, String contentType, long size, String sha256) {
    }

    record UploadResult(long size, String sha256) {
    }

    record CommitResult(String artifactId, long size, String sha256, String state, Instant readyAt) {
    }
}

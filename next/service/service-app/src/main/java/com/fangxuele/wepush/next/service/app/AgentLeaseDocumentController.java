package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RemoteRunCoordinator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
final class AgentLeaseDocumentController {
    private final RemoteRunCoordinator remoteRuns;
    private final AgentHttpAuthenticator authenticator;

    AgentLeaseDocumentController(RemoteRunCoordinator remoteRuns, AgentHttpAuthenticator authenticator) {
        this.remoteRuns = remoteRuns;
        this.authenticator = authenticator;
    }

    @GetMapping("/internal/agent/v1/leases/{leaseId}/execution-spec")
    ResponseEntity<byte[]> executionSpec(
            @PathVariable String leaseId,
            @RequestHeader(value = "x-wepush-agent-token", required = false) String supplied,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticator.requireForLease(leaseId, supplied, authorization);
        return json(load(leaseId, true));
    }

    @GetMapping("/internal/agent/v1/leases/{leaseId}/audience")
    ResponseEntity<byte[]> audience(
            @PathVariable String leaseId,
            @RequestHeader(value = "x-wepush-agent-token", required = false) String supplied,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticator.requireForLease(leaseId, supplied, authorization);
        return json(load(leaseId, false));
    }

    private static ResponseEntity<byte[]> json(byte[] value) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(value);
    }

    private byte[] load(String leaseId, boolean executionSpec) {
        try {
            return executionSpec ? remoteRuns.executionSpec(leaseId) : remoteRuns.audience(leaseId);
        } catch (RemoteRunCoordinator.RemoteProtocolProblem problem) {
            boolean missing = problem.code().contains("UNKNOWN") || problem.code().contains("NOT_FOUND");
            throw new ResponseStatusException(missing ? NOT_FOUND : GONE, problem.getMessage(), problem);
        }
    }
}

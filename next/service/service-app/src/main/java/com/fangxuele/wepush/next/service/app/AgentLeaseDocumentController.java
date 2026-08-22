package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RemoteRunCoordinator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
final class AgentLeaseDocumentController {
    private final RemoteRunCoordinator remoteRuns;
    private final String token;

    AgentLeaseDocumentController(RemoteRunCoordinator remoteRuns,
                                 @Value("${wepush.agent.grpc.token:}") String token) {
        this.remoteRuns = remoteRuns;
        this.token = token == null ? "" : token;
    }

    @GetMapping("/internal/agent/v1/leases/{leaseId}/execution-spec")
    ResponseEntity<byte[]> executionSpec(
            @PathVariable String leaseId,
            @RequestHeader(value = "x-wepush-agent-token", required = false) String supplied) {
        authorize(supplied);
        return json(load(leaseId, true));
    }

    @GetMapping("/internal/agent/v1/leases/{leaseId}/audience")
    ResponseEntity<byte[]> audience(
            @PathVariable String leaseId,
            @RequestHeader(value = "x-wepush-agent-token", required = false) String supplied) {
        authorize(supplied);
        return json(load(leaseId, false));
    }

    private void authorize(String supplied) {
        if (token.isBlank()) return;
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (supplied == null ? "" : supplied).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Agent token is invalid");
        }
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

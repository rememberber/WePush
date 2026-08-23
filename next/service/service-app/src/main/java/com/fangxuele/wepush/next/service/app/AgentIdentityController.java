package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.AgentIdentityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@RestController
final class AgentIdentityController {
    private final AgentIdentityService identities;

    AgentIdentityController(AgentIdentityService identities) {
        this.identities = identities;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/agent-enrollment-tokens")
    @ResponseStatus(HttpStatus.CREATED)
    EnrollmentTokenResponse createEnrollment(
            @org.springframework.web.bind.annotation.PathVariable String workspaceId,
            @RequestBody CreateEnrollmentRequest request) {
        var created = identities.createEnrollment(request.name(), workspaceId,
                Duration.parse(request.ttl()));
        return new EnrollmentTokenResponse(created.id(), created.token(), created.expiresAt());
    }

    @PostMapping("/internal/agent/v1/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    EnrollmentResponse enroll(
            @RequestHeader("Authorization") String authorization,
            @RequestBody EnrollmentRequest request) {
        try {
            return response(identities.enroll(scheme(authorization, "Enrollment"),
                    request.requestedAgentId(), request.publicKeyBase64()));
        } catch (AgentIdentityService.InvalidAgentCredentialException problem) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, problem.getMessage(), problem);
        }
    }

    @PostMapping("/internal/agent/v1/credentials/rotate")
    EnrollmentResponse rotate(
            @RequestHeader("Authorization") String authorization,
            @RequestBody RotateCredentialRequest request) {
        try {
            return response(identities.rotate(scheme(authorization, "Agent"),
                    request.publicKeyBase64()));
        } catch (AgentIdentityService.InvalidAgentCredentialException problem) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, problem.getMessage(), problem);
        }
    }

    private static String scheme(String authorization, String expected) {
        String prefix = expected + " ";
        if (authorization == null || !authorization.startsWith(prefix)
                || authorization.length() == prefix.length()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    expected + " authorization is required");
        }
        return authorization.substring(prefix.length()).trim();
    }

    private static EnrollmentResponse response(AgentIdentityService.EnrollmentResult result) {
        return new EnrollmentResponse(result.agentId(), result.credential(),
                result.credentialExpiresAt(), result.certificatePem(),
                result.caCertificatePem(), result.certificateExpiresAt());
    }

    record CreateEnrollmentRequest(String name, String ttl) {
    }

    record EnrollmentRequest(String requestedAgentId, String publicKeyBase64) {
    }

    record RotateCredentialRequest(String publicKeyBase64) {
    }

    record EnrollmentTokenResponse(String id, String token, Instant expiresAt) {
    }

    record EnrollmentResponse(String agentId, String credential, Instant credentialExpiresAt,
                              String certificatePem, String caCertificatePem,
                              Instant certificateExpiresAt) {
    }
}

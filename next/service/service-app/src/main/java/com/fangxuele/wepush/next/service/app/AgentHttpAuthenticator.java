package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.AgentIdentityService;
import com.fangxuele.wepush.next.service.domain.AgentLease;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
final class AgentHttpAuthenticator {
    private final AgentIdentityService identities;
    private final AgentLeaseRepository leases;
    private final byte[] legacyToken;
    private final boolean allowAnonymous;

    AgentHttpAuthenticator(AgentIdentityService identities, AgentLeaseRepository leases,
                           @Value("${wepush.agent.grpc.token:}") String legacyToken,
                           @Value("${server.address:127.0.0.1}") String serverAddress) {
        this.identities = identities;
        this.leases = leases;
        this.legacyToken = (legacyToken == null ? "" : legacyToken)
                .getBytes(StandardCharsets.UTF_8);
        this.allowAnonymous = AgentGrpcServer.isLoopback(serverAddress);
    }

    void requireForLease(String leaseId, String tokenHeader, String authorization) {
        String authenticatedAgent = authenticate(tokenHeader, authorization);
        if (authenticatedAgent.isEmpty()) return;
        AgentLease lease = leases.findById(leaseId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease is unknown"));
        if (!authenticatedAgent.equals(lease.agentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Agent identity does not own this Lease");
        }
    }

    String authenticate(String tokenHeader, String authorization) {
        String presented = tokenHeader == null ? "" : tokenHeader.trim();
        if (authorization != null && authorization.startsWith("Agent ")) {
            presented = authorization.substring("Agent ".length()).trim();
        }
        byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
        if (legacyToken.length > 0 && MessageDigest.isEqual(legacyToken, actual)) return "";
        if (!presented.isEmpty()) {
            try {
                return identities.authenticate(presented).agentId();
            } catch (AgentIdentityService.InvalidAgentCredentialException problem) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Agent credential is invalid", problem);
            }
        }
        if (allowAnonymous && legacyToken.length == 0) return "";
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Agent credential is required");
    }
}

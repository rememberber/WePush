package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.ApiAccessService;
import com.fangxuele.wepush.next.service.domain.ApiAccessRepository;
import com.fangxuele.wepush.next.service.domain.AuditEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
final class ApiAccessController {
    private final ApiAccessService access;
    private final AuditEventRepository audits;

    ApiAccessController(ApiAccessService access, AuditEventRepository audits) {
        this.access = access;
        this.audits = audits;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/api-tokens")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse create(@PathVariable String workspaceId,
                         @RequestBody CreateTokenRequest request) {
        var issued = access.create(request.name(), workspaceId,
                ApiAccessRepository.Role.valueOf(request.role()), Duration.parse(request.ttl()));
        return new TokenResponse(issued.tokenId(), issued.principalId(), issued.token(),
                issued.expiresAt(), issued.workspaceId(), issued.role().name());
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/api-tokens")
    List<TokenSummaryResponse> tokens(@PathVariable String workspaceId) {
        return access.list(workspaceId).stream().map(value -> new TokenSummaryResponse(value.id(),
                value.principalId(), value.name(), value.expiresAt(), value.revokedAt(),
                value.createdAt(), value.lastUsedAt())).toList();
    }

    @DeleteMapping("/api/v1/workspaces/{workspaceId}/api-tokens/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable String workspaceId, @PathVariable String tokenId) {
        access.revoke(tokenId, workspaceId);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/audit-events")
    List<AuditResponse> audits(@org.springframework.web.bind.annotation.PathVariable String workspaceId,
                               @RequestParam(defaultValue = "100") int limit) {
        return audits.list(workspaceId, limit).stream().map(value -> new AuditResponse(value.id(),
                value.workspaceId(), value.actorType(), value.actorId(), value.action(),
                value.resourceType(), value.resourceId(), value.result(), value.details().value(),
                value.occurredAt())).toList();
    }

    record CreateTokenRequest(String name, String role, String ttl) {
    }

    record TokenResponse(String tokenId, String principalId, String token, Instant expiresAt,
                         String workspaceId, String role) {
    }

    record TokenSummaryResponse(String tokenId, String principalId, String name,
                                Instant expiresAt, Instant revokedAt, Instant createdAt,
                                Instant lastUsedAt) {
    }

    record AuditResponse(String id, String workspaceId, String actorType, String actorId,
                         String action, String resourceType, String resourceId, String result,
                         String detailsJson, Instant occurredAt) {
    }
}

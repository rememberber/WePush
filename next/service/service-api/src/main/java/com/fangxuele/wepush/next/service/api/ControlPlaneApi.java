package com.fangxuele.wepush.next.service.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ControlPlaneApi {
    private ControlPlaneApi() {
    }

    public record CreateAccountRequest(String name, String providerId, String providerVersion,
                                       Object configuration) {
    }

    public record AccountResponse(String id, String workspaceId, String name, String providerId,
                                  String providerVersion, Object configuration, String status,
                                  Instant createdAt, Instant updatedAt, long version) {
    }

    public record CreateMessageRequest(String name, String providerId, String providerVersion,
                                       Object content) {
    }

    public record MessageResponse(String id, String workspaceId, String name, String providerId,
                                  String providerVersion, int revision, String schemaVersion,
                                  Object content, String contentHash, String status,
                                  Instant createdAt, Instant updatedAt, long version) {
    }

    public record CreateAudienceRequest(String name, List<RecipientRequest> recipients) {
    }

    public record RecipientRequest(String itemId, Object fields) {
    }

    public record SecretWriteRequest(char[] value) {
        @Override
        public String toString() {
            return "SecretWriteRequest[value=********]";
        }
    }

    public record SecretMetadataResponse(String workspaceId, String namespace, String name,
                                         String secretVersion, boolean configured, long recordVersion,
                                         Instant createdAt, Instant updatedAt) {
    }

    public record AudienceResponse(String id, String workspaceId, String name, String snapshotId,
                                   int revision, long recordCount, String contentHash, String status,
                                   Instant createdAt, Instant updatedAt, long version) {
    }

    public record CreateJobRequest(String name, String accountId, String messageId, String audienceId,
                                   Object policies, Boolean enabled) {
    }

    public record JobResponse(String id, String workspaceId, String name, String accountId,
                              String messageId, String audienceId, Object policies, boolean enabled,
                              Instant createdAt, Instant updatedAt, long version) {
    }

    public record CreateRunRequest(Boolean dryRun, Object policyOverrides, String reason) {
    }

    public record RunResponse(String id, String workspaceId, String jobId, String state,
                              String stateReason, boolean dryRun, RunCounters counters,
                              Instant createdAt, Instant startedAt, Instant endedAt, Instant updatedAt,
                              long version, Map<String, String> links) {
    }

    public record RunCounters(long total, long succeeded, long failed, long unknown,
                              long unsent, long skipped, long retried) {
    }

    public record RunEventResponse(String runId, long sequence, String type, Instant occurredAt,
                                   Object payload, String severity) {
    }

    public record RunItemResultResponse(String runId, String itemId, int attempts, String state,
                                        String providerCode, String diagnostic, String externalRequestId,
                                        Instant completedAt, Object metadata) {
    }

    public record CursorPage(String nextCursor, boolean hasMore) {
    }

    public record RunItemResultPage(List<RunItemResultResponse> items, CursorPage page) {
    }

    public record CancelRunRequest(String reason) {
    }

    public record ChangeConcurrencyRequest(int target) {
    }

    public record RunCommandResponse(String commandId, String type, String status, String code,
                                     String message, Instant acknowledgedAt, boolean replayed) {
    }

    public record ArtifactResponse(String id, String workspaceId, String runId, String type,
                                   String backend, String originalName, String contentType,
                                   long size, String sha256, String state, Instant expiresAt,
                                   boolean pinned, boolean legalHold, Instant createdAt,
                                   Instant readyAt, Instant deletedAt, long version,
                                   Map<String, String> links) {
    }

    public record ArtifactCleanupResponse(int claimed, int deleted, int failed) {
    }
}

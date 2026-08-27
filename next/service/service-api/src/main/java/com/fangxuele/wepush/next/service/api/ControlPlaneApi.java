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

    public record UpdateAccountRequest(String name, Object configuration, String status) { }
    public record ConnectionTestRequest(String timeout) { }
    public record ConnectionTestResponse(boolean successful, String code, String diagnostic,
                                         long latencyMillis) { }

    public record CreateMessageRequest(String name, String providerId, String providerVersion,
                                       Object content) {
    }

    public record MessageResponse(String id, String workspaceId, String name, String providerId,
                                  String providerVersion, int revision, String schemaVersion,
                                  Object content, String contentHash, String status,
                                  Instant createdAt, Instant updatedAt, long version) {
    }

    public record UpdateMessageRequest(String name, Object content, String status) { }
    public record CopyResourceRequest(String name) { }
    public record MessageRevisionResponse(String messageId, int revision, String schemaVersion,
                                          Object content, String contentHash, Instant createdAt) { }
    public record RevisionPage(List<MessageRevisionResponse> items, Integer nextBeforeRevision,
                               boolean hasMore) { }
    public record MessageDiffResponse(MessageRevisionResponse from, MessageRevisionResponse to,
                                      List<String> changedPaths) { }

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

    public record UpdateAudienceRequest(String name, List<RecipientRequest> recipients, String status) { }
    public record AudienceImportRowResponse(long sequence, String itemId, Object fields, String rawLine,
                                            boolean accepted, String errorCode, String errorMessage) { }
    public record AudienceImportResponse(String id, String workspaceId, String audienceId, String name,
                                         String format, String itemIdColumn, Object fieldMapping, String status,
                                         long totalRows, long acceptedRows, long rejectedRows, long duplicateRows,
                                         List<AudienceImportRowResponse> acceptedPreview,
                                         List<AudienceImportRowResponse> errorPreview,
                                         Instant createdAt, Instant updatedAt, String errorsUrl) { }

    public record CreateJobRequest(String name, String accountId, String messageId, String audienceId,
                                   Object policies, Boolean enabled) {
    }

    public record JobResponse(String id, String workspaceId, String name, String accountId,
                              String messageId, String audienceId, Object policies, boolean enabled,
                              boolean archived, String status,
                              Instant createdAt, Instant updatedAt, long version) {
    }

    public record UpdateJobRequest(String name, String accountId, String messageId, String audienceId,
                                   Object policies, Boolean enabled, Boolean archived) { }

    public record CreateRunRequest(Boolean dryRun, Object policyOverrides, String reason,
                                   String confirmationToken) {
    }

    public record LiveConfirmationResponse(String jobId, String jobName, String providerId,
                                           String providerVersion, String accountId, String accountName,
                                           String audienceId, String audienceName, long audienceCount,
                                           Object policies, int targetConcurrency, long rateLimitPermits,
                                           String rateLimitPeriod, long estimatedItems, Instant expiresAt,
                                           String confirmationToken) { }
    public record RetryRequest(java.util.Set<String> states, String confirmationToken) { }
    public record RetryConfirmationResponse(String sourceRunId, java.util.Set<String> states,
                                             long itemCount, Instant expiresAt,
                                             String confirmationToken) { }

    public record RunResponse(String id, String workspaceId, String jobId, String jobName, String state,
                              String stateReason, boolean dryRun, RunCounters counters,
                              String sourceRunId, String retryStates,
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

    public record ResourcePageResponse<T>(List<T> items, CursorPage page) { }

    public record RunTrendResponse(String day, long total, long succeeded, long problem) { }
    public record OverviewResponse(long activeRuns, long totalRuns, long succeededRuns, long problemRuns,
                                   List<RunResponse> active, List<RunResponse> recent,
                                   List<RunTrendResponse> trend) { }

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

    public record AgentProviderResponse(String providerId, String implementationVersion,
                                        int spiMajor, int maximumConcurrency) {
    }

    public record AgentResponse(String id, String status, String agentVersion, int protocolVersion,
                                String operatingSystem, String architecture, String javaVersion,
                                int maximumRuns, int activeRuns, int availableRuns,
                                List<AgentProviderResponse> providers, String sessionId,
                                long lastAgentSequence, long lastServiceSequence,
                                Instant connectedAt, Instant lastSeenAt, Instant disconnectedAt,
                                long version, Map<String, String> links) {
    }
}

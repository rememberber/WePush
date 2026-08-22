package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record ArtifactDefinition(
        String id,
        WorkspaceId workspaceId,
        String runId,
        String type,
        String backend,
        String location,
        String originalName,
        String contentType,
        long size,
        String sha256,
        State state,
        Instant expiresAt,
        boolean pinned,
        boolean legalHold,
        Instant createdAt,
        Instant readyAt,
        Instant deletedAt,
        String lastError,
        long version
) {
    public ArtifactDefinition {
        id = DomainChecks.text(id, "artifact id");
        type = DomainChecks.text(type, "artifact type");
        backend = DomainChecks.text(backend, "artifact backend");
        location = DomainChecks.text(location, "artifact location");
        originalName = DomainChecks.text(originalName, "artifact original name");
        contentType = DomainChecks.text(contentType, "artifact content type");
        runId = runId == null || runId.isBlank() ? null : runId.trim();
        sha256 = sha256 == null ? "" : sha256;
        lastError = lastError == null ? "" : lastError;
        if (workspaceId == null || state == null || expiresAt == null || createdAt == null
                || expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("artifact definition is incomplete");
        }
        DomainChecks.nonNegative(size, "artifact size");
        DomainChecks.nonNegative(version, "artifact version");
        if (state == State.READY && (readyAt == null || sha256.length() != 64)) {
            throw new IllegalArgumentException("ready artifact requires timestamp and SHA-256");
        }
        if (state == State.DELETED && deletedAt == null) {
            throw new IllegalArgumentException("deleted artifact requires timestamp");
        }
    }

    public enum State { UPLOADING, READY, DELETING, DELETED, FAILED }
}

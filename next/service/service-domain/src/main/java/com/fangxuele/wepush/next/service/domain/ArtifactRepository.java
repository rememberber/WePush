package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ArtifactRepository {
    void create(ArtifactDefinition artifact);

    Optional<ArtifactDefinition> findById(WorkspaceId workspaceId, String artifactId);

    Optional<ArtifactDefinition> findReadyByRunAndType(WorkspaceId workspaceId, String runId, String type);

    List<ArtifactDefinition> listForRun(WorkspaceId workspaceId, String runId);

    void markReady(WorkspaceId workspaceId, String artifactId, long size, String sha256, Instant readyAt);

    void recordUpload(WorkspaceId workspaceId, String artifactId, long size, String sha256);

    void markFailed(WorkspaceId workspaceId, String artifactId, String error);

    List<ArtifactDefinition> claimExpired(Instant now, int limit);

    void markDeleted(WorkspaceId workspaceId, String artifactId, Instant deletedAt);

    default void createMultipart(ArtifactMultipartUpload upload) {
        throw new UnsupportedOperationException("Multipart Artifact metadata is unavailable");
    }

    default Optional<ArtifactMultipartUpload> findMultipart(WorkspaceId workspaceId, String artifactId) {
        return Optional.empty();
    }

    default void deleteMultipart(WorkspaceId workspaceId, String artifactId) {
    }
}

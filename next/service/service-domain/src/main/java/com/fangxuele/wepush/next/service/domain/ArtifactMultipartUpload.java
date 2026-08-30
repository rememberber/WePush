package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record ArtifactMultipartUpload(String artifactId, WorkspaceId workspaceId, String uploadId,
                                      long partSize, int partCount, Instant createdAt) {
    public ArtifactMultipartUpload {
        if (artifactId == null || artifactId.isBlank() || workspaceId == null
                || uploadId == null || uploadId.isBlank() || uploadId.length() > 1024
                || partSize < 5L * 1024L * 1024L || partCount < 1 || partCount > 10_000
                || createdAt == null) throw new IllegalArgumentException("Artifact multipart upload is invalid");
    }
}

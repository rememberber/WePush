package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface ArtifactStore {
    ObjectPlan plan(WorkspaceId workspaceId, String artifactId, String type, Instant createdAt);

    StoredObject write(ObjectPlan plan, ContentWriter writer) throws IOException;

    StoredObject inspect(String location) throws IOException;

    default Optional<PresignedUpload> presignUpload(ObjectPlan plan, long size, String sha256,
                                                    String contentType, Instant expiresAt) {
        return Optional.empty();
    }

    InputStream open(String location, long offset, long length) throws IOException;

    void delete(String location) throws IOException;

    record ObjectPlan(String backend, String location) {
    }

    record StoredObject(long size, String sha256) {
    }

    record PresignedUpload(String url, Map<String, String> headers) {
        public PresignedUpload {
            headers = Map.copyOf(headers);
        }
    }

    @FunctionalInterface
    interface ContentWriter {
        void write(OutputStream output) throws IOException;
    }
}

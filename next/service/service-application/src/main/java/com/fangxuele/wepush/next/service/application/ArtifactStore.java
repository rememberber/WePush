package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public interface ArtifactStore {
    ObjectPlan plan(WorkspaceId workspaceId, String artifactId, String type, Instant createdAt);

    StoredObject write(ObjectPlan plan, ContentWriter writer) throws IOException;

    StoredObject inspect(String location) throws IOException;

    default Optional<PresignedUpload> presignUpload(ObjectPlan plan, long size, String sha256,
                                                    String contentType, Instant expiresAt) {
        return Optional.empty();
    }

    default Optional<MultipartUpload> beginMultipartUpload(ObjectPlan plan, long size, String sha256,
                                                           String contentType, Instant expiresAt)
            throws IOException {
        return Optional.empty();
    }

    default List<PresignedPart> presignMultipartParts(ObjectPlan plan, String uploadId,
                                                      int firstPartNumber, int count,
                                                      long totalSize, long partSize,
                                                      Instant expiresAt) throws IOException {
        throw new IOException("Artifact Store does not support presigned multipart uploads");
    }

    default StoredObject completeMultipartUpload(ObjectPlan plan, String uploadId,
                                                 List<CompletedUploadPart> parts) throws IOException {
        throw new IOException("Artifact Store does not support presigned multipart uploads");
    }

    default void abortMultipartUpload(ObjectPlan plan, String uploadId) throws IOException {
        throw new IOException("Artifact Store does not support presigned multipart uploads");
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

    record MultipartUpload(String uploadId, long partSize, int partCount) {
        public MultipartUpload {
            if (uploadId == null || uploadId.isBlank() || partSize < 5L * 1024L * 1024L
                    || partCount < 1 || partCount > 10_000) {
                throw new IllegalArgumentException("Multipart upload plan is invalid");
            }
        }
    }

    record PresignedPart(int partNumber, long offset, long size, String url,
                         Map<String, String> headers) {
        public PresignedPart {
            if (partNumber < 1 || partNumber > 10_000 || offset < 0 || size < 1
                    || url == null || url.isBlank()) throw new IllegalArgumentException("Multipart part is invalid");
            headers = Map.copyOf(headers);
        }
    }

    record CompletedUploadPart(int partNumber, String eTag) {
        public CompletedUploadPart {
            if (partNumber < 1 || partNumber > 10_000 || eTag == null || eTag.isBlank()
                    || eTag.length() > 256) throw new IllegalArgumentException("Completed multipart part is invalid");
        }
    }

    @FunctionalInterface
    interface ContentWriter {
        void write(OutputStream output) throws IOException;
    }
}

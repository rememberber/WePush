package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.service.domain.AgentLease;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import com.fangxuele.wepush.next.service.domain.ArtifactDefinition;
import com.fangxuele.wepush.next.service.domain.ArtifactRepository;
import com.fangxuele.wepush.next.service.domain.ArtifactMultipartUpload;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AgentArtifactApplicationService {
    private final AgentLeaseRepository leases;
    private final ArtifactRepository artifacts;
    private final RunRepository runs;
    private final WorkspaceResourceGovernor resources;
    private final ArtifactStore store;
    private final ArtifactUploadTokenCodec tokens;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final JsonCodec json;
    private final RunEventPublisher events;
    private final Clock clock;
    private final String publicBaseUrl;
    private final Duration uploadTtl;
    private final Duration retention;
    private final long maximumBytes;
    private final long multipartThresholdBytes;

    public AgentArtifactApplicationService(AgentLeaseRepository leases, ArtifactRepository artifacts,
                                           RunRepository runs, WorkspaceResourceGovernor resources,
                                           ArtifactStore store,
                                           ArtifactUploadTokenCodec tokens, ResourceIdGenerator ids,
                                           TransactionRunner transactions, JsonCodec json,
                                           RunEventPublisher events, Clock clock, String publicBaseUrl,
                                           Duration uploadTtl, Duration retention, long maximumBytes,
                                           long multipartThresholdBytes) {
        this.leases = leases;
        this.artifacts = artifacts;
        this.runs = runs;
        this.resources = resources;
        this.store = store;
        this.tokens = tokens;
        this.ids = ids;
        this.transactions = transactions;
        this.json = json;
        this.events = events;
        this.clock = clock;
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) throw new IllegalArgumentException("public URL required");
        this.publicBaseUrl = publicBaseUrl.replaceFirst("/+$", "");
        if (uploadTtl == null || uploadTtl.isNegative() || uploadTtl.isZero()
                || retention == null || retention.isNegative() || retention.isZero()
                || maximumBytes < 1 || multipartThresholdBytes < 5L * 1024L * 1024L
                || multipartThresholdBytes >= maximumBytes) {
            throw new IllegalArgumentException("Artifact upload limits are invalid");
        }
        this.uploadTtl = uploadTtl;
        this.retention = retention;
        this.maximumBytes = maximumBytes;
        this.multipartThresholdBytes = multipartThresholdBytes;
    }

    public UploadPlan create(LeaseFence fence, String type, String originalName, String contentType,
                             long expectedSize, String expectedSha256) {
        AgentLease lease = requireLease(fence);
        String safeType = safe(type, "type", 80);
        String safeName = safeFileName(originalName);
        String safeContentType = safeContentType(contentType);
        String sha256 = expectedSha256 == null ? "" : expectedSha256.toLowerCase();
        if (expectedSize < 0 || expectedSize > maximumBytes || !sha256.matches("[0-9a-f]{64}")) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "ARTIFACT_DIGEST_INVALID",
                    "Artifact expected size or SHA-256 is invalid");
        }
        Instant now = clock.instant();
        Instant tokenExpiry = now.plus(uploadTtl);
        String artifactId = ids.next("artifact");
        ArtifactStore.ObjectPlan storage = store.plan(lease.workspaceId(), artifactId, safeType, now);
        var policy = resources.policy(lease.workspaceId());
        Duration workspaceRetention = policy.version() == 0 ? retention : policy.artifactRetention();
        ArtifactDefinition uploading = new ArtifactDefinition(artifactId, lease.workspaceId(), lease.runId(),
                safeType, storage.backend(), storage.location(), safeName, safeContentType,
                expectedSize, sha256, ArtifactDefinition.State.UPLOADING, now.plus(workspaceRetention),
                false, false, now, null, null, "", 0);
        transactions.required(() -> {
            resources.requireArtifactCapacity(lease.workspaceId(), expectedSize);
            artifacts.create(uploading);
        });
        String token = tokens.issue(artifactId, lease.id(), expectedSize, sha256, tokenExpiry);
        String base = publicBaseUrl + "/internal/agent/v1/artifacts/"
                + URLEncoder.encode(artifactId, StandardCharsets.UTF_8);
        String query = "?upload_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        if (expectedSize > multipartThresholdBytes) {
            ArtifactStore.MultipartUpload multipart = null;
            try {
                multipart = store.beginMultipartUpload(storage, expectedSize,
                        sha256, safeContentType, tokenExpiry).orElseThrow(() ->
                        new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE,
                                "ARTIFACT_MULTIPART_UNAVAILABLE",
                                "Artifacts above the single-upload threshold require an S3 multipart Store"));
                ArtifactStore.MultipartUpload planned = multipart;
                transactions.required(() -> artifacts.createMultipart(new ArtifactMultipartUpload(
                        artifactId, lease.workspaceId(), planned.uploadId(), planned.partSize(),
                        planned.partCount(), now)));
                MultipartPlan multipartPlan = new MultipartPlan(planned.uploadId(), planned.partSize(),
                        planned.partCount(), base + "/multipart-parts" + query,
                        base + "/multipart-complete" + query, base + "/multipart" + query);
                return new UploadPlan(artifactId, multipartPlan.partPlanUrl(), base + "/commit" + query,
                        tokenExpiry, expectedSize, sha256, Map.of(), "MULTIPART", multipartPlan);
            } catch (ApplicationProblem problem) {
                abortPlannedMultipart(storage, multipart);
                transactions.required(() -> artifacts.markFailed(lease.workspaceId(), artifactId,
                        problem.code()));
                throw problem;
            } catch (IOException problem) {
                abortPlannedMultipart(storage, multipart);
                transactions.required(() -> artifacts.markFailed(lease.workspaceId(), artifactId,
                        "MULTIPART_PLAN_FAILURE"));
                throw new IllegalStateException("Multipart Artifact plan failed", problem);
            } catch (RuntimeException problem) {
                abortPlannedMultipart(storage, multipart);
                transactions.required(() -> artifacts.markFailed(lease.workspaceId(), artifactId,
                        "MULTIPART_PLAN_FAILURE"));
                throw problem;
            }
        }
        ArtifactStore.PresignedUpload direct = store.presignUpload(storage, expectedSize, sha256,
                safeContentType, tokenExpiry).orElse(null);
        String uploadUrl = direct == null ? base + "/content" + query : direct.url();
        Map<String, String> uploadHeaders = direct == null ? Map.of() : direct.headers();
        return new UploadPlan(artifactId, uploadUrl, base + "/commit" + query,
                tokenExpiry, expectedSize, sha256, uploadHeaders, "SINGLE", null);
    }

    private void abortPlannedMultipart(ArtifactStore.ObjectPlan plan,
                                       ArtifactStore.MultipartUpload multipart) {
        if (multipart == null) return;
        try {
            store.abortMultipartUpload(plan, multipart.uploadId());
        } catch (IOException | RuntimeException ignored) {
            // Cleanup will retry persisted sessions; an unpersisted orphan also expires by bucket policy.
        }
    }

    public List<ArtifactStore.PresignedPart> multipartParts(String artifactId, String token,
                                                            String uploadId, int firstPartNumber,
                                                            int count) {
        MultipartContext context = multipartContext(artifactId, token, uploadId);
        try {
            return store.presignMultipartParts(context.plan(), uploadId, firstPartNumber, count,
                    context.claims().expectedSize(), context.upload().partSize(),
                    context.claims().expiresAt());
        } catch (IOException problem) {
            throw new IllegalStateException("Multipart Artifact parts could not be presigned", problem);
        }
    }

    public ArtifactStore.StoredObject completeMultipart(String artifactId, String token, String uploadId,
                                                        List<ArtifactStore.CompletedUploadPart> parts) {
        MultipartContext context = multipartContext(artifactId, token, uploadId);
        if (parts == null || parts.size() != context.upload().partCount()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "MULTIPART_PARTS_INCOMPLETE",
                    "Completed multipart upload must contain every planned part exactly once");
        }
        for (int index = 0; index < parts.size(); index++) {
            if (parts.get(index).partNumber() != index + 1) {
                throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST,
                        "MULTIPART_PARTS_OUT_OF_ORDER", "Multipart completion parts must be ordered and contiguous");
            }
        }
        try {
            ArtifactStore.StoredObject stored = store.completeMultipartUpload(context.plan(), uploadId, parts);
            if (stored.size() != context.claims().expectedSize()
                    || !stored.sha256().equals(context.claims().expectedSha256())) {
                store.delete(context.artifact().location());
                transactions.required(() -> artifacts.markFailed(context.lease().workspaceId(), artifactId,
                        "MULTIPART_INTEGRITY_MISMATCH"));
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT,
                        "ARTIFACT_INTEGRITY_MISMATCH", "Completed multipart Artifact differs from its plan");
            }
            transactions.required(() -> {
                artifacts.recordUpload(context.lease().workspaceId(), artifactId,
                        stored.size(), stored.sha256());
                artifacts.deleteMultipart(context.lease().workspaceId(), artifactId);
            });
            return stored;
        } catch (ApplicationProblem problem) {
            throw problem;
        } catch (IOException problem) {
            throw new IllegalStateException("Multipart Artifact completion failed", problem);
        }
    }

    public void abortMultipart(String artifactId, String token, String uploadId) {
        MultipartContext context = multipartContext(artifactId, token, uploadId);
        try {
            store.abortMultipartUpload(context.plan(), uploadId);
        } catch (IOException problem) {
            throw new IllegalStateException("Multipart Artifact abort failed", problem);
        }
        transactions.required(() -> {
            artifacts.markFailed(context.lease().workspaceId(), artifactId, "MULTIPART_ABORTED");
            artifacts.deleteMultipart(context.lease().workspaceId(), artifactId);
        });
    }

    public ArtifactStore.StoredObject upload(String artifactId, String token, InputStream input) {
        ArtifactUploadTokenCodec.Claims claims = claims(artifactId, token);
        AgentLease lease = requireLease(claims.leaseId());
        ArtifactDefinition artifact = requireArtifact(lease, artifactId);
        try {
            ArtifactStore.StoredObject stored = store.write(
                    new ArtifactStore.ObjectPlan(artifact.backend(), artifact.location()),
                    output -> copyExact(input, output, claims.expectedSize()));
            if (stored.size() != claims.expectedSize()
                    || !stored.sha256().equals(claims.expectedSha256())) {
                store.delete(artifact.location());
                transactions.required(() -> artifacts.markFailed(lease.workspaceId(), artifactId,
                        "UPLOAD_CHECKSUM_MISMATCH"));
                throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST,
                        "ARTIFACT_CHECKSUM_MISMATCH", "Uploaded Artifact checksum does not match the plan");
            }
            transactions.required(() -> artifacts.recordUpload(lease.workspaceId(), artifactId,
                    stored.size(), stored.sha256()));
            return stored;
        } catch (ApplicationProblem problem) {
            throw problem;
        } catch (IOException problem) {
            transactions.required(() -> artifacts.markFailed(lease.workspaceId(), artifactId,
                    "UPLOAD_IO_FAILURE"));
            throw new IllegalStateException("Agent Artifact upload failed", problem);
        }
    }

    public ArtifactDefinition commit(String artifactId, String token) {
        ArtifactUploadTokenCodec.Claims claims = claims(artifactId, token);
        AgentLease lease = requireLease(claims.leaseId());
        ArtifactDefinition artifact = requireArtifact(lease, artifactId);
        ArtifactStore.StoredObject stored;
        try {
            stored = store.inspect(artifact.location());
        } catch (IOException problem) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "ARTIFACT_UPLOAD_MISSING",
                    "Artifact upload has not completed");
        }
        if (stored.size() != claims.expectedSize() || !stored.sha256().equals(claims.expectedSha256())
                || artifact.size() != stored.size() || !artifact.sha256().equals(stored.sha256())) {
            transactions.required(() -> artifacts.markFailed(lease.workspaceId(), artifactId,
                    "COMMIT_INTEGRITY_MISMATCH"));
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "ARTIFACT_INTEGRITY_MISMATCH",
                    "Artifact object and metadata do not match");
        }
        RunEventRecord ready = transactions.required(() -> {
            Instant readyAt = clock.instant();
            artifacts.markReady(lease.workspaceId(), artifactId, stored.size(), stored.sha256(), readyAt);
            RunEventRecord event = new RunEventRecord(lease.runId(), lease.workspaceId(),
                    runs.nextEventSequence(lease.workspaceId(), lease.runId()), "ARTIFACT_READY", readyAt,
                    json.canonicalize(Map.of("artifactId", artifactId, "type", artifact.type(),
                            "size", stored.size(), "sha256", stored.sha256(), "source", "AGENT")),
                    RunEventRecord.Severity.INFO);
            runs.appendEvent(event);
            return event;
        });
        events.publish(ready);
        return artifacts.findById(lease.workspaceId(), artifactId)
                .orElseThrow(() -> new IllegalStateException("committed Artifact metadata is missing"));
    }

    private ArtifactUploadTokenCodec.Claims claims(String artifactId, String token) {
        ArtifactUploadTokenCodec.Claims claims = tokens.verify(token, clock.instant());
        if (!claims.artifactId().equals(artifactId)) {
            throw new ArtifactUploadTokenCodec.InvalidUploadTokenException("Artifact upload token target differs");
        }
        return claims;
    }

    private AgentLease requireLease(LeaseFence fence) {
        AgentLease lease = requireLease(fence.leaseId());
        if (!lease.runId().equals(fence.runId()) || lease.epoch() != fence.epoch()
                || !lease.fencingToken().equals(fence.fencingToken())) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "LEASE_FENCE_STALE",
                    "Artifact request uses stale Lease authority");
        }
        return lease;
    }

    private AgentLease requireLease(String leaseId) {
        AgentLease lease = leases.findById(leaseId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "LEASE_UNKNOWN", "Lease is unknown"));
        if (!lease.status().active() || !lease.expiresAt().isAfter(clock.instant())) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "LEASE_INACTIVE",
                    "Lease no longer accepts Artifact uploads");
        }
        return lease;
    }

    private ArtifactDefinition requireArtifact(AgentLease lease, String artifactId) {
        ArtifactDefinition artifact = artifacts.findById(lease.workspaceId(), artifactId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "ARTIFACT_NOT_FOUND",
                        "Artifact upload is unknown"));
        if (!lease.runId().equals(artifact.runId()) || artifact.state() != ArtifactDefinition.State.UPLOADING) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "ARTIFACT_NOT_UPLOADING",
                    "Artifact is not uploadable for this Lease");
        }
        return artifact;
    }

    private MultipartContext multipartContext(String artifactId, String token, String uploadId) {
        ArtifactUploadTokenCodec.Claims claims = claims(artifactId, token);
        AgentLease lease = requireLease(claims.leaseId());
        ArtifactDefinition artifact = requireArtifact(lease, artifactId);
        ArtifactMultipartUpload upload = artifacts.findMultipart(lease.workspaceId(), artifactId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "MULTIPART_UPLOAD_UNKNOWN",
                        "Artifact does not have an active multipart upload"));
        if (uploadId == null || !upload.uploadId().equals(uploadId)) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "MULTIPART_UPLOAD_MISMATCH",
                    "Multipart upload identifier does not match the Artifact plan");
        }
        return new MultipartContext(claims, lease, artifact, upload,
                new ArtifactStore.ObjectPlan(artifact.backend(), artifact.location()));
    }

    private static void copyExact(InputStream input, OutputStream output, long expected) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long remaining = expected;
        while (remaining > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) throw new IOException("Artifact body ended before Content-Length");
            if (read == 0) continue;
            output.write(buffer, 0, read);
            remaining -= read;
        }
        if (input.read() != -1) throw new IOException("Artifact body exceeds the upload plan");
    }

    private static String safe(String value, String label, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum
                || !value.matches("[A-Za-z0-9._-]+")) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "ARTIFACT_" + label.toUpperCase()
                    + "_INVALID", "Artifact " + label + " is invalid");
        }
        return value;
    }

    private static String safeFileName(String value) {
        return safe(value, "name", 180);
    }

    private static String safeContentType(String value) {
        if (value == null || value.isBlank() || value.length() > 160 || value.contains("\r")
                || value.contains("\n") || !value.matches("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+(?:;[ A-Za-z0-9=._-]+)?")) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "ARTIFACT_CONTENT_TYPE_INVALID",
                    "Artifact content type is invalid");
        }
        return value;
    }

    public record UploadPlan(String artifactId, String uploadUrl, String commitUrl, Instant expiresAt,
                             long expectedSize, String expectedSha256,
                             Map<String, String> uploadHeaders, String uploadMode,
                             MultipartPlan multipart) {
    }

    public record MultipartPlan(String uploadId, long partSize, int partCount,
                                String partPlanUrl, String completeUrl, String abortUrl) { }

    private record MultipartContext(ArtifactUploadTokenCodec.Claims claims, AgentLease lease,
                                    ArtifactDefinition artifact, ArtifactMultipartUpload upload,
                                    ArtifactStore.ObjectPlan plan) { }
}

package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class S3ArtifactStoreIntegrationTest {
    private static final long MULTIPART_SIZE = 100L * 1024L * 1024L;

    @Test
    void supportsSignedUploadRangeDeleteAndMultipartAgainstCompatibleStorage() throws Exception {
        String endpoint = System.getenv("WEPUSH_TEST_S3_ENDPOINT");
        assumeTrue(endpoint != null && !endpoint.isBlank(), "WEPUSH_TEST_S3_ENDPOINT is not configured");
        String accessKey = environment("WEPUSH_TEST_S3_ACCESS_KEY", "wepush-test");
        String secretKey = environment("WEPUSH_TEST_S3_SECRET_KEY", "wepush-test-secret");
        String bucket = "wepush-test-" + UUID.randomUUID();
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        try (S3Client administration = S3Client.builder().endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1).credentialsProvider(credentials)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            administration.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            try (S3ArtifactStore store = new S3ArtifactStore(new S3ArtifactStore.Configuration(
                    bucket, "us-east-1", endpoint, true, accessKey, secretKey,
                    "integration", "NONE"))) {
                verifyServiceWriteAndRange(store);
                verifyPresignedUpload(store);
                verifyMultipart(store);
            } finally {
                administration.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
            }
        }
    }

    private static void verifyServiceWriteAndRange(S3ArtifactStore store) throws Exception {
        byte[] content = "wepush-s3-range-check".getBytes(StandardCharsets.UTF_8);
        ArtifactStore.ObjectPlan plan = store.plan(new WorkspaceId("ws_default"),
                "artifact_service_write", "RUN_RESULTS_CSV", Instant.now());
        ArtifactStore.StoredObject stored = store.write(plan, output -> output.write(content));
        assertEquals(content.length, stored.size());
        try (var input = store.open(plan.location(), 7, 8)) {
            assertArrayEquals("s3-range".getBytes(StandardCharsets.UTF_8), input.readAllBytes());
        }
        store.delete(plan.location());
    }

    private static void verifyPresignedUpload(S3ArtifactStore store) throws Exception {
        byte[] content = "agent-direct-upload".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(content);
        ArtifactStore.ObjectPlan plan = store.plan(new WorkspaceId("ws_default"),
                "artifact_presigned", "AGENT_RESULT", Instant.now());
        ArtifactStore.PresignedUpload upload = store.presignUpload(plan, content.length, sha256,
                "application/octet-stream", Instant.now().plusSeconds(300)).orElseThrow();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(upload.url()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        upload.headers().forEach(request::header);
        HttpResponse<Void> response = HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.discarding());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "presigned upload returned HTTP " + response.statusCode());
        assertEquals(new ArtifactStore.StoredObject(content.length, sha256),
                store.inspect(plan.location()));
        store.delete(plan.location());
    }

    private static void verifyMultipart(S3ArtifactStore store) throws Exception {
        ArtifactStore.ObjectPlan plan = store.plan(new WorkspaceId("ws_default"),
                "artifact_multipart", "LARGE_RESULT", Instant.now());
        byte[] block = new byte[1024 * 1024];
        ArtifactStore.StoredObject stored = store.write(plan, output -> {
            for (int index = 0; index < 100; index++) output.write(block);
        });
        assertEquals(MULTIPART_SIZE, stored.size());
        assertEquals(stored, store.inspect(plan.location()));
        store.delete(plan.location());
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}

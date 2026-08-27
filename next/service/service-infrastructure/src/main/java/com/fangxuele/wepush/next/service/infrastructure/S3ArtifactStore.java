package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;

public final class S3ArtifactStore implements ArtifactStore, AutoCloseable {
    private static final String SHA_METADATA = "wepush-sha256";
    private static final long MULTIPART_THRESHOLD = 100L * 1024L * 1024L;
    private static final long PART_SIZE = 16L * 1024L * 1024L;
    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private final String bucket;
    private final String environment;
    private final Encryption encryption;
    private final S3Client client;
    private final S3Presigner presigner;

    public S3ArtifactStore(Configuration configuration) {
        if (configuration == null || configuration.bucket() == null
                || configuration.bucket().isBlank()) {
            throw new IllegalArgumentException("S3 Artifact bucket is required");
        }
        this.bucket = configuration.bucket();
        this.environment = safeSegment(configuration.environment(), "environment");
        this.encryption = Encryption.resolve(configuration.serverSideEncryption());
        var credentials = configuration.accessKey() == null || configuration.accessKey().isBlank()
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(
                configuration.accessKey(), configuration.secretKey()));
        S3Configuration service = S3Configuration.builder()
                .pathStyleAccessEnabled(configuration.pathStyleAccess())
                .checksumValidationEnabled(true).build();
        var clientBuilder = S3Client.builder().region(Region.of(configuration.region()))
                .credentialsProvider(credentials).serviceConfiguration(service)
                .httpClientBuilder(UrlConnectionHttpClient.builder());
        var presignerBuilder = S3Presigner.builder().region(Region.of(configuration.region()))
                .credentialsProvider(credentials).serviceConfiguration(service);
        if (configuration.endpoint() != null && !configuration.endpoint().isBlank()) {
            URI endpoint = URI.create(configuration.endpoint());
            clientBuilder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @Override
    public ObjectPlan plan(WorkspaceId workspaceId, String artifactId, String type, Instant createdAt) {
        String workspace = safeSegment(workspaceId.value(), "workspace");
        String artifact = safeSegment(artifactId, "artifact");
        String artifactType = safeSegment(type.toLowerCase(), "artifact type");
        String year = DateTimeFormatter.ofPattern("uuuu").withZone(ZoneOffset.UTC).format(createdAt);
        String month = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC).format(createdAt);
        return new ObjectPlan("S3", environment + "/" + workspace + "/" + artifactType
                + "/" + year + "/" + month + "/" + artifact);
    }

    @Override
    public StoredObject write(ObjectPlan plan, ContentWriter writer) throws IOException {
        Path temporary = Files.createTempFile("wepush-s3-artifact-", ".tmp");
        try {
            if (Files.getFileAttributeView(temporary, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(temporary, OWNER_ONLY);
            }
            MessageDigest digest = sha256();
            try (var output = new DigestOutputStream(Files.newOutputStream(temporary), digest)) {
                writer.write(output);
            }
            long size = Files.size(temporary);
            String sha256 = HexFormat.of().formatHex(digest.digest());
            if (size >= MULTIPART_THRESHOLD) multipartUpload(plan.location(), temporary, size, sha256,
                    "application/octet-stream");
            else client.putObject(put(plan.location(), size, sha256, "application/octet-stream"),
                    RequestBody.fromFile(temporary));
            StoredObject stored = inspect(plan.location());
            if (stored.size() != size || !stored.sha256().equals(sha256)) {
                delete(plan.location());
                throw new IOException("S3 Artifact integrity differs after upload");
            }
            return stored;
        } catch (S3Exception problem) {
            throw io("S3 Artifact upload failed", problem);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public StoredObject inspect(String location) throws IOException {
        try {
            var response = client.headObject(HeadObjectRequest.builder().bucket(bucket)
                    .key(location).checksumMode(ChecksumMode.ENABLED).build());
            String sha256 = response.metadata().getOrDefault(SHA_METADATA, "");
            if (sha256.isBlank() && response.checksumSHA256() != null) {
                sha256 = HexFormat.of().formatHex(Base64.getDecoder().decode(response.checksumSHA256()));
            }
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IOException("S3 Artifact object does not expose a trusted SHA-256 checksum");
            }
            return new StoredObject(response.contentLength(), sha256);
        } catch (S3Exception problem) {
            throw io(problem.statusCode() == 404 ? "S3 Artifact does not exist"
                    : "S3 Artifact Head failed", problem);
        }
    }

    @Override
    public Optional<PresignedUpload> presignUpload(ObjectPlan plan, long size, String sha256,
                                                   String contentType, Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("S3 presigned upload expiry is invalid");
        }
        PutObjectRequest object = put(plan.location(), size, sha256, contentType);
        var signed = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(duration).putObjectRequest(object).build());
        Map<String, String> headers = new LinkedHashMap<>();
        signed.signedHeaders().forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name) && !"content-length".equalsIgnoreCase(name)) {
                headers.put(name, String.join(",", values));
            }
        });
        return Optional.of(new PresignedUpload(signed.url().toString(), headers));
    }

    @Override
    public InputStream open(String location, long offset, long length) throws IOException {
        if (length == 0) return InputStream.nullInputStream();
        try {
            String range = "bytes=" + offset + "-" + (offset + length - 1);
            ResponseInputStream<?> response = client.getObject(GetObjectRequest.builder()
                    .bucket(bucket).key(location).range(range).checksumMode(ChecksumMode.ENABLED).build());
            return response;
        } catch (S3Exception problem) {
            throw io("S3 Artifact download failed", problem);
        }
    }

    @Override
    public void delete(String location) throws IOException {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(location).build());
        } catch (S3Exception problem) {
            throw io("S3 Artifact delete failed", problem);
        }
    }

    private PutObjectRequest put(String key, long size, String sha256, String contentType) {
        String checksum = Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256));
        PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(key)
                .contentLength(size).contentType(contentType).checksumSHA256(checksum)
                .metadata(Map.of(SHA_METADATA, sha256));
        applyEncryption(request);
        return request.build();
    }

    private void multipartUpload(String key, Path file, long size, String sha256,
                                 String contentType) throws IOException {
        CreateMultipartUploadRequest.Builder create = CreateMultipartUploadRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).metadata(Map.of(SHA_METADATA, sha256));
        applyEncryption(create);
        String uploadId = client.createMultipartUpload(create.build()).uploadId();
        List<CompletedPart> completed = new ArrayList<>();
        try {
            long offset = 0;
            int partNumber = 1;
            while (offset < size) {
                long length = Math.min(PART_SIZE, size - offset);
                long partOffset = offset;
                RequestBody body = RequestBody.fromContentProvider(
                        () -> new BoundedFileInputStream(file, partOffset, length), length, contentType);
                var uploaded = client.uploadPart(UploadPartRequest.builder().bucket(bucket).key(key)
                        .uploadId(uploadId).partNumber(partNumber).contentLength(length).build(), body);
                completed.add(CompletedPart.builder().partNumber(partNumber).eTag(uploaded.eTag()).build());
                offset += length;
                partNumber++;
            }
            client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket).key(key).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completed).build()).build());
        } catch (RuntimeException problem) {
            try {
                client.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket)
                        .key(key).uploadId(uploadId).build());
            } catch (RuntimeException ignored) {
                problem.addSuppressed(ignored);
            }
            throw problem;
        }
    }

    private static final class BoundedFileInputStream extends InputStream {
        private final FileChannel channel;
        private long remaining;

        private BoundedFileInputStream(Path path, long offset, long length) {
            try {
                channel = FileChannel.open(path, StandardOpenOption.READ);
                channel.position(offset);
                remaining = length;
            } catch (IOException problem) {
                throw new IllegalStateException("S3 multipart source cannot be opened", problem);
            }
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (remaining == 0) return -1;
            int maximum = (int) Math.min(length, remaining);
            int read = channel.read(ByteBuffer.wrap(bytes, offset, maximum));
            if (read > 0) remaining -= read;
            return read;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private void applyEncryption(PutObjectRequest.Builder request) {
        if (encryption == Encryption.AES256) request.serverSideEncryption(ServerSideEncryption.AES256);
    }

    private void applyEncryption(CreateMultipartUploadRequest.Builder request) {
        if (encryption == Encryption.AES256) request.serverSideEncryption(ServerSideEncryption.AES256);
    }

    private static IOException io(String message, S3Exception problem) {
        return new IOException(message + " (HTTP " + problem.statusCode() + ")", problem);
    }

    private static String safeSegment(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9._-]{1,160}")) {
            throw new IllegalArgumentException("S3 " + label + " segment is invalid");
        }
        return value;
    }

    @Override
    public void close() {
        presigner.close();
        client.close();
    }

    public record Configuration(String bucket, String region, String endpoint,
                                boolean pathStyleAccess, String accessKey, String secretKey,
                                String environment, String serverSideEncryption) {
        public Configuration {
            if (region == null || region.isBlank()) region = "us-east-1";
            if (environment == null || environment.isBlank()) environment = "server";
            if (accessKey != null && !accessKey.isBlank()
                    && (secretKey == null || secretKey.isBlank())) {
                throw new IllegalArgumentException("S3 secret key is required with an access key");
            }
        }
    }

    private enum Encryption {
        NONE,
        AES256;

        private static Encryption resolve(String configured) {
            String value = configured == null || configured.isBlank()
                    ? "AUTO" : configured.trim().toUpperCase(java.util.Locale.ROOT);
            return "AUTO".equals(value)
                    ? AES256
                    : switch (value) {
                        case "NONE" -> NONE;
                        case "AES256", "SSE_S3" -> AES256;
                        default -> throw new IllegalArgumentException(
                                "S3 server-side encryption must be AUTO, NONE, or AES256");
                    };
        }
    }
}

package com.fangxuele.wepush.next.service.application;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public final class ArtifactUploadTokenCodec {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final byte[] key;

    public ArtifactUploadTokenCodec(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("Artifact upload signing key must be at least 256 bits");
        }
        this.key = key.clone();
    }

    public String issue(String artifactId, String leaseId, long expectedSize, String expectedSha256,
                        Instant expiresAt) {
        validate(artifactId, leaseId, expectedSize, expectedSha256, expiresAt);
        byte[] payload = payload(artifactId, leaseId, expectedSize, expectedSha256, expiresAt);
        return ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(mac(payload));
    }

    public Claims verify(String token, Instant now) {
        if (token == null || token.length() > 4096) throw invalid();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) throw invalid();
        try {
            byte[] payload = DECODER.decode(parts[0]);
            byte[] signature = DECODER.decode(parts[1]);
            if (!ENCODER.encodeToString(payload).equals(parts[0])
                    || !ENCODER.encodeToString(signature).equals(parts[1])
                    || !MessageDigest.isEqual(mac(payload), signature)) throw invalid();
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            String artifactId = text(buffer);
            String leaseId = text(buffer);
            long expectedSize = buffer.getLong();
            String sha256 = text(buffer);
            Instant expiresAt = Instant.ofEpochSecond(buffer.getLong());
            if (buffer.hasRemaining()) throw invalid();
            validate(artifactId, leaseId, expectedSize, sha256, expiresAt);
            if (!expiresAt.isAfter(now)) throw new InvalidUploadTokenException("Artifact upload token expired");
            return new Claims(artifactId, leaseId, expectedSize, sha256, expiresAt);
        } catch (InvalidUploadTokenException problem) {
            throw problem;
        } catch (RuntimeException problem) {
            throw invalid();
        }
    }

    private byte[] payload(String artifactId, String leaseId, long expectedSize,
                           String expectedSha256, Instant expiresAt) {
        byte[] artifact = artifactId.getBytes(StandardCharsets.UTF_8);
        byte[] lease = leaseId.getBytes(StandardCharsets.UTF_8);
        byte[] sha = expectedSha256.getBytes(StandardCharsets.US_ASCII);
        return ByteBuffer.allocate(Short.BYTES + artifact.length + Short.BYTES + lease.length
                        + Long.BYTES + Short.BYTES + sha.length + Long.BYTES)
                .putShort((short) artifact.length).put(artifact)
                .putShort((short) lease.length).put(lease)
                .putLong(expectedSize).putShort((short) sha.length).put(sha)
                .putLong(expiresAt.getEpochSecond()).array();
    }

    private static String text(ByteBuffer buffer) {
        int length = Short.toUnsignedInt(buffer.getShort());
        if (length < 1 || length > buffer.remaining()) throw invalid();
        byte[] value = new byte[length];
        buffer.get(value);
        return new String(value, StandardCharsets.UTF_8);
    }

    private byte[] mac(byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("HMAC-SHA-256 is unavailable", impossible);
        }
    }

    private static void validate(String artifactId, String leaseId, long expectedSize,
                                 String sha256, Instant expiresAt) {
        if (artifactId == null || artifactId.isBlank() || artifactId.length() > 255
                || leaseId == null || leaseId.isBlank() || leaseId.length() > 255
                || expectedSize < 0 || sha256 == null || !sha256.matches("[0-9a-f]{64}")
                || expiresAt == null) throw invalid();
    }

    private static InvalidUploadTokenException invalid() {
        return new InvalidUploadTokenException("Artifact upload token is invalid");
    }

    public record Claims(String artifactId, String leaseId, long expectedSize,
                         String expectedSha256, Instant expiresAt) {
    }

    public static final class InvalidUploadTokenException extends RuntimeException {
        InvalidUploadTokenException(String message) {
            super(message);
        }
    }
}

package com.fangxuele.wepush.next.service.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactUploadTokenCodecTest {
    @Test
    void bindsClaimsRejectsTamperingAndExpiry() {
        ArtifactUploadTokenCodec codec = new ArtifactUploadTokenCodec(new byte[32]);
        Instant expiry = Instant.parse("2026-08-23T10:00:00Z");
        String token = codec.issue("artifact-1", "lease-1", 12, "a".repeat(64), expiry);

        assertEquals("artifact-1", codec.verify(token, expiry.minusSeconds(1)).artifactId());
        char replacement = token.charAt(token.length() - 1) == 'A' ? 'B' : 'A';
        assertThrows(ArtifactUploadTokenCodec.InvalidUploadTokenException.class,
                () -> codec.verify(token.substring(0, token.length() - 1) + replacement,
                        expiry.minusSeconds(1)));
        assertThrows(ArtifactUploadTokenCodec.InvalidUploadTokenException.class,
                () -> codec.verify(token, expiry));
    }
}

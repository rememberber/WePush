package com.fangxuele.wepush.next.agent.protocol;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretEnvelopeCodecTest {
    private final SecretEnvelopeCodec codec = new SecretEnvelopeCodec();
    private final LeaseFence fence = new LeaseFence("lease_1", "run_1", 3, "fence-token");
    private final Instant expiry = Instant.parse("2026-08-23T01:01:00Z");
    private final Instant now = Instant.parse("2026-08-23T01:00:00Z");

    @Test
    void encryptsForOneAgentAndLeaseAndClearsOpenedValues() {
        KeyPair recipient = codec.generateRecipientKeyPair();
        SecretEnvelopeCodec.SecretMaterial source = new SecretEnvelopeCodec.SecretMaterial(
                "http", "authorization", "v1", "must-not-leak".getBytes(StandardCharsets.UTF_8));
        byte[] envelope;
        try (source) {
            envelope = codec.seal("agent_1", fence, expiry,
                    codec.encodePublicKey(recipient.getPublic()), List.of(source));
        }

        SecretEnvelopeCodec.SecretMaterial opened;
        try (SecretEnvelopeCodec.OpenedSecrets secrets = codec.open("agent_1", fence, expiry, now,
                recipient.getPrivate(), envelope)) {
            assertEquals(1, secrets.secrets().size());
            opened = secrets.secrets().getFirst();
            assertEquals("http", opened.namespace());
            assertArrayEquals("must-not-leak".getBytes(StandardCharsets.UTF_8), opened.copyValue());
        }
        assertThrows(IllegalStateException.class, opened::copyValue);
    }

    @Test
    void rejectsTamperingWrongBindingWrongKeyAndExpiry() {
        KeyPair recipient = codec.generateRecipientKeyPair();
        byte[] envelope;
        try (SecretEnvelopeCodec.SecretMaterial source = new SecretEnvelopeCodec.SecretMaterial(
                "http", "authorization", "v1", "value".getBytes(StandardCharsets.UTF_8))) {
            envelope = codec.seal("agent_1", fence, expiry,
                    codec.encodePublicKey(recipient.getPublic()), List.of(source));
        }

        byte[] tampered = envelope.clone();
        tampered[tampered.length - 1] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> codec.open(
                "agent_1", fence, expiry, now, recipient.getPrivate(), tampered));
        assertThrows(IllegalArgumentException.class, () -> codec.open(
                "agent_2", fence, expiry, now, recipient.getPrivate(), envelope));
        assertThrows(IllegalArgumentException.class, () -> codec.open(
                "agent_1", new LeaseFence("lease_2", "run_1", 3, "fence-token"),
                expiry, now, recipient.getPrivate(), envelope));
        KeyPair other = codec.generateRecipientKeyPair();
        assertThrows(IllegalArgumentException.class, () -> codec.open(
                "agent_1", fence, expiry, now, other.getPrivate(), envelope));
        assertThrows(IllegalArgumentException.class, () -> codec.open(
                "agent_1", fence, expiry, expiry, recipient.getPrivate(), envelope));
    }
}

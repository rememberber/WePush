package com.fangxuele.wepush.next.agent.protocol;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Lease-bound secret transport using ephemeral X25519, HKDF-SHA-256 and AES-256-GCM.
 * The recipient private key and decrypted values are intentionally never serializable.
 */
public final class SecretEnvelopeCodec {
    private static final byte[] OUTER_MAGIC = "WPSENV01".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PAYLOAD_MAGIC = "WPSSEC01".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] HKDF_INFO = "wepush-agent-secret-envelope-v1"
            .getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = 1;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MAXIMUM_ENVELOPE_BYTES = 512 * 1024;
    private static final int MAXIMUM_FIELD_BYTES = 64 * 1024;

    private final SecureRandom random = new SecureRandom();

    public KeyPair generateRecipientKeyPair() {
        try {
            return KeyPairGenerator.getInstance("X25519").generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("X25519 is unavailable", exception);
        }
    }

    public String encodePublicKey(PublicKey key) {
        if (key == null || !"XDH".equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("an X25519 public key is required");
        }
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public byte[] seal(String agentId, LeaseFence fence, Instant expiresAt,
                       String recipientPublicKey, List<SecretMaterial> secrets) {
        requireBinding(agentId, fence, expiresAt);
        if (recipientPublicKey == null || recipientPublicKey.isBlank()) {
            throw new IllegalArgumentException("Agent secret encryption public key is required");
        }
        if (secrets == null) throw new IllegalArgumentException("secrets are required");
        if (secrets.isEmpty()) return new byte[0];

        byte[] binding = binding(agentId, fence, expiresAt);
        byte[] clear = payload(agentId, fence, expiresAt, secrets);
        byte[] shared = null;
        byte[] key = null;
        try {
            PublicKey recipient = decodePublicKey(recipientPublicKey);
            KeyPair ephemeral = generateRecipientKeyPair();
            byte[] ephemeralPublic = ephemeral.getPublic().getEncoded();
            shared = agreement(ephemeral.getPrivate(), recipient);
            key = deriveKey(shared, binding);
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            byte[] ciphertext = crypt(Cipher.ENCRYPT_MODE, clear, key, nonce, binding, ephemeralPublic);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(output)) {
                data.write(OUTER_MAGIC);
                data.writeInt(VERSION);
                writeBytes(data, ephemeralPublic, 256);
                writeBytes(data, nonce, NONCE_BYTES);
                writeBytes(data, ciphertext, MAXIMUM_ENVELOPE_BYTES);
            }
            byte[] envelope = output.toByteArray();
            if (envelope.length > MAXIMUM_ENVELOPE_BYTES) {
                throw new IllegalArgumentException("secret envelope exceeds the protocol limit");
            }
            return envelope;
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("secret envelope could not be encoded", exception);
        } finally {
            Arrays.fill(binding, (byte) 0);
            Arrays.fill(clear, (byte) 0);
            if (shared != null) Arrays.fill(shared, (byte) 0);
            if (key != null) Arrays.fill(key, (byte) 0);
        }
    }

    public OpenedSecrets open(String agentId, LeaseFence fence, Instant expiresAt, Instant now,
                              PrivateKey recipientPrivateKey, byte[] envelope) {
        requireBinding(agentId, fence, expiresAt);
        if (now == null || recipientPrivateKey == null) {
            throw new IllegalArgumentException("Agent time and private key are required");
        }
        if (!now.isBefore(expiresAt)) throw new IllegalArgumentException("secret envelope has expired");
        if (envelope == null || envelope.length == 0 || envelope.length > MAXIMUM_ENVELOPE_BYTES) {
            throw new IllegalArgumentException("secret envelope is empty or exceeds the protocol limit");
        }
        byte[] ephemeralPublic;
        byte[] nonce;
        byte[] ciphertext;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(envelope))) {
            requireMagic(input, OUTER_MAGIC, "secret envelope");
            if (input.readInt() != VERSION) {
                throw new IllegalArgumentException("secret envelope version is unsupported");
            }
            ephemeralPublic = readBytes(input, 256, "ephemeral public key");
            nonce = readBytes(input, NONCE_BYTES, "nonce");
            if (nonce.length != NONCE_BYTES) throw new IllegalArgumentException("secret envelope nonce is invalid");
            ciphertext = readBytes(input, MAXIMUM_ENVELOPE_BYTES, "ciphertext");
            if (input.available() != 0) throw new IllegalArgumentException("secret envelope has trailing data");
        } catch (IOException exception) {
            throw invalid("secret envelope is truncated", exception);
        }
        byte[] binding = binding(agentId, fence, expiresAt);
        byte[] shared = null;
        byte[] key = null;
        byte[] clear = null;
        try {
            PublicKey ephemeral = KeyFactory.getInstance("X25519")
                    .generatePublic(new X509EncodedKeySpec(ephemeralPublic));
            shared = agreement(recipientPrivateKey, ephemeral);
            key = deriveKey(shared, binding);
            clear = crypt(Cipher.DECRYPT_MODE, ciphertext, key, nonce, binding, ephemeralPublic);
            return parsePayload(clear, agentId, fence, expiresAt, now);
        } catch (GeneralSecurityException exception) {
            throw invalid("secret envelope authentication failed", exception);
        } finally {
            Arrays.fill(binding, (byte) 0);
            if (shared != null) Arrays.fill(shared, (byte) 0);
            if (key != null) Arrays.fill(key, (byte) 0);
            if (clear != null) Arrays.fill(clear, (byte) 0);
        }
    }

    private static byte[] payload(String agentId, LeaseFence fence, Instant expiresAt,
                                  List<SecretMaterial> secrets) {
        List<SecretMaterial> ordered = secrets.stream()
                .sorted(Comparator.comparing(SecretMaterial::identity)).toList();
        try (WipingByteArrayOutputStream output = new WipingByteArrayOutputStream()) {
            DataOutputStream data = new DataOutputStream(output);
            data.write(PAYLOAD_MAGIC);
            data.writeInt(VERSION);
            writeString(data, agentId);
            writeString(data, fence.leaseId());
            writeString(data, fence.runId());
            data.writeLong(fence.epoch());
            writeString(data, fence.fencingToken());
            data.writeLong(expiresAt.getEpochSecond());
            data.writeInt(expiresAt.getNano());
            data.writeInt(ordered.size());
            for (SecretMaterial secret : ordered) {
                writeString(data, secret.namespace());
                writeString(data, secret.name());
                writeString(data, secret.version());
                byte[] value = secret.copyValue();
                try {
                    writeBytes(data, value);
                } finally {
                    Arrays.fill(value, (byte) 0);
                }
            }
            data.flush();
            byte[] result = output.toByteArray();
            if (result.length > MAXIMUM_ENVELOPE_BYTES) {
                throw new IllegalArgumentException("secret payload exceeds the protocol limit");
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("secret payload could not be encoded", exception);
        }
    }

    private static OpenedSecrets parsePayload(byte[] clear, String agentId, LeaseFence fence,
                                              Instant expiresAt, Instant now) {
        List<SecretMaterial> values = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(clear))) {
            requireMagic(input, PAYLOAD_MAGIC, "secret payload");
            if (input.readInt() != VERSION
                    || !agentId.equals(readString(input, "agent ID"))
                    || !fence.leaseId().equals(readString(input, "lease ID"))
                    || !fence.runId().equals(readString(input, "run ID"))
                    || fence.epoch() != input.readLong()
                    || !fence.fencingToken().equals(readString(input, "fencing token"))) {
                throw new IllegalArgumentException("secret envelope binding does not match this lease");
            }
            Instant payloadExpiry = Instant.ofEpochSecond(input.readLong(), input.readInt());
            if (!expiresAt.equals(payloadExpiry) || !now.isBefore(payloadExpiry)) {
                throw new IllegalArgumentException("secret envelope has expired or its expiry binding differs");
            }
            int count = input.readInt();
            if (count < 0 || count > 10_000) throw new IllegalArgumentException("secret count is invalid");
            for (int index = 0; index < count; index++) {
                String namespace = readString(input, "namespace");
                String name = readString(input, "name");
                String version = readString(input, "version");
                byte[] value = readBytes(input, MAXIMUM_FIELD_BYTES, "secret value");
                try {
                    values.add(new SecretMaterial(namespace, name, version, value));
                } finally {
                    Arrays.fill(value, (byte) 0);
                }
            }
            if (input.available() != 0) throw new IllegalArgumentException("secret payload has trailing data");
            return new OpenedSecrets(values);
        } catch (IOException | RuntimeException exception) {
            values.forEach(SecretMaterial::close);
            if (exception instanceof IllegalArgumentException invalid) throw invalid;
            throw invalid("secret payload is invalid", exception);
        }
    }

    private static PublicKey decodePublicKey(String value) {
        try {
            byte[] encoded = Base64.getDecoder().decode(value);
            if (encoded.length > 256) throw new IllegalArgumentException("Agent public key is too large");
            return KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw invalid("Agent secret encryption public key is invalid", exception);
        }
    }

    private static byte[] agreement(PrivateKey privateKey, PublicKey publicKey)
            throws GeneralSecurityException {
        KeyAgreement agreement = KeyAgreement.getInstance("X25519");
        agreement.init(privateKey);
        agreement.doPhase(publicKey, true);
        return agreement.generateSecret();
    }

    private static byte[] deriveKey(byte[] shared, byte[] binding) {
        try {
            byte[] salt = MessageDigest.getInstance("SHA-256").digest(binding);
            Mac extract = Mac.getInstance("HmacSHA256");
            extract.init(new SecretKeySpec(salt, "HmacSHA256"));
            byte[] prk = extract.doFinal(shared);
            Mac expand = Mac.getInstance("HmacSHA256");
            expand.init(new SecretKeySpec(prk, "HmacSHA256"));
            expand.update(HKDF_INFO);
            expand.update(binding);
            expand.update((byte) 1);
            byte[] key = expand.doFinal();
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(prk, (byte) 0);
            return key;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HKDF-SHA-256 is unavailable", exception);
        }
    }

    private static byte[] crypt(int mode, byte[] input, byte[] key, byte[] nonce,
                                byte[] binding, byte[] ephemeralPublic)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(binding);
        cipher.updateAAD(ephemeralPublic);
        return cipher.doFinal(input);
    }

    private static byte[] binding(String agentId, LeaseFence fence, Instant expiresAt) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(output)) {
                writeString(data, agentId);
                writeString(data, fence.leaseId());
                writeString(data, fence.runId());
                data.writeLong(fence.epoch());
                writeString(data, fence.fencingToken());
                data.writeLong(expiresAt.getEpochSecond());
                data.writeInt(expiresAt.getNano());
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("lease binding could not be encoded", exception);
        }
    }

    private static void requireBinding(String agentId, LeaseFence fence, Instant expiresAt) {
        if (agentId == null || agentId.isBlank() || fence == null || expiresAt == null) {
            throw new IllegalArgumentException("Agent, lease fence, and expiry are required");
        }
    }

    private static void requireMagic(DataInputStream input, byte[] expected, String name)
            throws IOException {
        byte[] actual = input.readNBytes(expected.length);
        if (!MessageDigest.isEqual(actual, expected)) {
            throw new IllegalArgumentException(name + " magic is invalid");
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        if (value == null) throw new IllegalArgumentException("secret identity field is null");
        writeBytes(output, value.getBytes(StandardCharsets.UTF_8), 65_536);
    }

    private static String readString(DataInputStream input, String name) throws IOException {
        return new String(readBytes(input, 65_536, name), StandardCharsets.UTF_8);
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        writeBytes(output, value, MAXIMUM_FIELD_BYTES);
    }

    private static void writeBytes(DataOutputStream output, byte[] value, int maximum) throws IOException {
        if (value.length > maximum) {
            throw new IllegalArgumentException("secret envelope field exceeds the protocol limit");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input, int maximum, String name) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > maximum) {
            throw new IllegalArgumentException(name + " length is invalid");
        }
        byte[] value = input.readNBytes(length);
        if (value.length != length) throw new EOFException(name + " is truncated");
        return value;
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }

    public static final class SecretMaterial implements AutoCloseable {
        private final String namespace;
        private final String name;
        private final String version;
        private byte[] value;

        public SecretMaterial(String namespace, String name, String version, byte[] value) {
            if (namespace == null || namespace.isBlank() || name == null || name.isBlank()
                    || version == null || version.isBlank() || value == null || value.length == 0) {
                throw new IllegalArgumentException("secret material is incomplete");
            }
            this.namespace = namespace;
            this.name = name;
            this.version = version;
            this.value = value.clone();
        }

        public String namespace() { return namespace; }
        public String name() { return name; }
        public String version() { return version; }

        public synchronized byte[] copyValue() {
            if (value.length == 0) throw new IllegalStateException("secret material is closed");
            return value.clone();
        }

        private String identity() {
            return namespace + '\0' + name + '\0' + version;
        }

        @Override
        public synchronized void close() {
            Arrays.fill(value, (byte) 0);
            value = new byte[0];
        }
    }

    public static final class OpenedSecrets implements AutoCloseable {
        private final List<SecretMaterial> secrets;

        private OpenedSecrets(List<SecretMaterial> secrets) {
            this.secrets = List.copyOf(secrets);
        }

        public List<SecretMaterial> secrets() { return secrets; }

        @Override
        public void close() { secrets.forEach(SecretMaterial::close); }
    }

    private static final class WipingByteArrayOutputStream extends ByteArrayOutputStream {
        @Override
        public void close() {
            Arrays.fill(buf, (byte) 0);
            reset();
        }
    }
}

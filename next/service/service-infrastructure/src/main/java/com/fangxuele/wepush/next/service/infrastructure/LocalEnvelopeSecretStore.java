package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.core.api.InMemorySecretValue;
import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.service.application.SecretMetadata;
import com.fangxuele.wepush.next.service.application.SecretStore;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public final class LocalEnvelopeSecretStore implements SecretStore, AutoCloseable {
    private static final String ALGORITHM = "AES-256-GCM-ENVELOPE-V1";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final JdbcTemplate jdbc;
    private final LocalMasterKeyRing keyRing;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public LocalEnvelopeSecretStore(JdbcTemplate jdbc, Path keyFile, String injectedMasterKey,
                                    boolean standalone, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
        Integer records = jdbc.queryForObject("SELECT COUNT(*) FROM secret_record", Integer.class);
        this.keyRing = LocalMasterKeyRing.open(keyFile, injectedMasterKey, standalone,
                records != null && records > 0);
    }

    @Override
    public SecretMetadata put(WorkspaceId workspaceId, SecretRef ref, char[] value) {
        if (workspaceId == null || ref == null || value == null || value.length == 0) {
            throw new IllegalArgumentException("secret identity and value are required");
        }
        StoredSecret previous = find(workspaceId, ref).orElse(null);
        long recordVersion = previous == null ? 1 : previous.recordVersion() + 1;
        Instant now = clock.instant();
        Instant createdAt = previous == null ? now : previous.createdAt();
        String keyVersion = keyRing.activeVersion();
        byte[] aad = aad(workspaceId, ref, recordVersion);
        byte[] clear = encode(value);
        byte[] dek = new byte[32];
        byte[] wrapAad = dekAad(aad, keyVersion);
        random.nextBytes(dek);
        try {
            Encrypted data = encrypt(clear, new SecretKeySpec(dek, "AES"), aad);
            Encrypted wrappedDek = encrypt(dek, keyRing.activeKey(), wrapAad);
            if (previous == null) {
                jdbc.update("""
                        INSERT INTO secret_record
                        (workspace_id, secret_namespace, secret_name, secret_version, record_version,
                         algorithm, key_version, ciphertext, data_nonce, encrypted_dek, dek_nonce,
                         created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, workspaceId.value(), ref.namespace(), ref.name(), ref.version(), recordVersion,
                        ALGORITHM, keyVersion, data.ciphertext(), data.nonce(),
                        wrappedDek.ciphertext(), wrappedDek.nonce(), createdAt.toString(), now.toString());
            } else {
                int changed = jdbc.update("""
                        UPDATE secret_record
                        SET record_version = ?, algorithm = ?, key_version = ?, ciphertext = ?, data_nonce = ?,
                            encrypted_dek = ?, dek_nonce = ?, updated_at = ?
                        WHERE workspace_id = ? AND secret_namespace = ? AND secret_name = ?
                          AND secret_version = ? AND record_version = ?
                        """, recordVersion, ALGORITHM, keyVersion, data.ciphertext(), data.nonce(),
                        wrappedDek.ciphertext(), wrappedDek.nonce(), now.toString(), workspaceId.value(),
                        ref.namespace(), ref.name(), ref.version(), previous.recordVersion());
                if (changed != 1) {
                    throw new IllegalStateException("Secret was concurrently modified");
                }
            }
            return new SecretMetadata(workspaceId, ref, true, recordVersion, createdAt, now);
        } finally {
            Arrays.fill(clear, (byte) 0);
            Arrays.fill(dek, (byte) 0);
            Arrays.fill(aad, (byte) 0);
            Arrays.fill(wrapAad, (byte) 0);
        }
    }

    @Override
    public Optional<SecretMetadata> metadata(WorkspaceId workspaceId, SecretRef ref) {
        return find(workspaceId, ref).map(value -> new SecretMetadata(workspaceId, ref, true,
                value.recordVersion(), value.createdAt(), value.updatedAt()));
    }

    @Override
    public SecretValue resolve(WorkspaceId workspaceId, SecretRef ref) {
        StoredSecret stored = find(workspaceId, ref)
                .orElseThrow(() -> new IllegalStateException("Secret is not configured"));
        if (!ALGORITHM.equals(stored.algorithm())) {
            throw new IllegalStateException("Secret encryption algorithm is unsupported");
        }
        byte[] aad = aad(workspaceId, ref, stored.recordVersion());
        byte[] dek = null;
        byte[] clear = null;
        char[] chars = null;
        byte[] wrapAad = dekAad(aad, stored.keyVersion());
        try {
            dek = decrypt(stored.encryptedDek(), stored.dekNonce(), keyRing.key(stored.keyVersion()),
                    wrapAad);
            clear = decrypt(stored.ciphertext(), stored.dataNonce(), new SecretKeySpec(dek, "AES"), aad);
            chars = decode(clear);
            return InMemorySecretValue.of(chars);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Secret cannot be decrypted", exception);
        } finally {
            if (dek != null) Arrays.fill(dek, (byte) 0);
            if (clear != null) Arrays.fill(clear, (byte) 0);
            if (chars != null) Arrays.fill(chars, '\0');
            Arrays.fill(aad, (byte) 0);
            Arrays.fill(wrapAad, (byte) 0);
        }
    }

    @Override
    public void close() {
        keyRing.close();
    }

    private Optional<StoredSecret> find(WorkspaceId workspaceId, SecretRef ref) {
        return jdbc.query("""
                SELECT * FROM secret_record
                WHERE workspace_id = ? AND secret_namespace = ? AND secret_name = ? AND secret_version = ?
                """, (rs, row) -> new StoredSecret(rs.getLong("record_version"), rs.getString("algorithm"),
                rs.getString("key_version"), rs.getBytes("ciphertext"), rs.getBytes("data_nonce"),
                rs.getBytes("encrypted_dek"), rs.getBytes("dek_nonce"),
                Instant.parse(rs.getString("created_at")), Instant.parse(rs.getString("updated_at"))),
                workspaceId.value(), ref.namespace(), ref.name(), ref.version()).stream().findFirst();
    }

    private Encrypted encrypt(byte[] clear, SecretKey key, byte[] aad) {
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad);
            return new Encrypted(cipher.doFinal(clear), nonce);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Secret encryption failed", exception);
        }
    }

    private static byte[] decrypt(byte[] encrypted, byte[] nonce, SecretKey key, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad);
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Secret authentication failed", exception);
        }
    }

    private static byte[] aad(WorkspaceId workspaceId, SecretRef ref, long recordVersion) {
        return (workspaceId.value() + "\u0000" + ref.namespace() + "\u0000" + ref.name() + "\u0000"
                + ref.version() + "\u0000" + recordVersion).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] dekAad(byte[] aad, String keyVersion) {
        byte[] suffix = ("\u0000dek\u0000" + keyVersion).getBytes(StandardCharsets.UTF_8);
        byte[] combined = Arrays.copyOf(aad, aad.length + suffix.length);
        System.arraycopy(suffix, 0, combined, aad.length, suffix.length);
        Arrays.fill(suffix, (byte) 0);
        return combined;
    }

    private static byte[] encode(char[] value) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Secret value is not valid Unicode", exception);
        }
    }

    private static char[] decode(byte[] value) {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(value));
            char[] chars = new char[decoded.remaining()];
            decoded.get(chars);
            if (decoded.hasArray()) Arrays.fill(decoded.array(), '\0');
            return chars;
        } catch (CharacterCodingException exception) {
            throw new IllegalStateException("Secret value cannot be decoded", exception);
        }
    }

    private record Encrypted(byte[] ciphertext, byte[] nonce) {
    }

    private record StoredSecret(long recordVersion, String algorithm, String keyVersion,
                                byte[] ciphertext, byte[] dataNonce, byte[] encryptedDek, byte[] dekNonce,
                                Instant createdAt, Instant updatedAt) {
    }
}

package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.CursorCodec;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public final class LocalHmacCursorCodec implements CursorCodec, AutoCloseable {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final LocalMasterKeyRing keyRing;

    public LocalHmacCursorCodec(Path keyFile, String injectedMasterKey, boolean standalone,
                                boolean encryptedRecordsExist) {
        this.keyRing = LocalMasterKeyRing.open(keyFile, injectedMasterKey, standalone, encryptedRecordsExist);
    }

    @Override
    public String encode(String purpose, String value) {
        byte[] payload = value.getBytes(StandardCharsets.UTF_8);
        byte[] signature = sign(purpose, payload);
        try {
            return ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(signature);
        } finally {
            Arrays.fill(payload, (byte) 0);
            Arrays.fill(signature, (byte) 0);
        }
    }

    @Override
    public String decode(String purpose, String cursor) {
        try {
            int separator = cursor.indexOf('.');
            if (separator < 1 || separator != cursor.lastIndexOf('.')) {
                throw new IllegalArgumentException("cursor format");
            }
            byte[] payload = DECODER.decode(cursor.substring(0, separator));
            byte[] supplied = DECODER.decode(cursor.substring(separator + 1));
            byte[] expected = sign(purpose, payload);
            try {
                if (!MessageDigest.isEqual(expected, supplied)) {
                    throw new IllegalArgumentException("cursor signature");
                }
                return new String(payload, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(payload, (byte) 0);
                Arrays.fill(supplied, (byte) 0);
                Arrays.fill(expected, (byte) 0);
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("cursor cannot be decoded", exception);
        }
    }

    @Override
    public void close() {
        keyRing.close();
    }

    private byte[] sign(String purpose, byte[] payload) {
        byte[] key = keyRing.activeKey().getEncoded();
        byte[] context = ("wepush-cursor\u0000" + purpose + "\u0000").getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(context);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("cursor signature cannot be created", exception);
        } finally {
            Arrays.fill(key, (byte) 0);
            Arrays.fill(context, (byte) 0);
        }
    }
}

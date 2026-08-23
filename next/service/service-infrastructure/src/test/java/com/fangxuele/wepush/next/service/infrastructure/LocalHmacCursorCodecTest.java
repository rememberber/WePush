package com.fangxuele.wepush.next.service.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalHmacCursorCodecTest {
    private static final String BASE64_URL =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    @TempDir
    Path directory;

    @Test
    void rejectsNonCanonicalBase64EvenWhenItDecodesToTheSameSignature() {
        try (LocalHmacCursorCodec codec = new LocalHmacCursorCodec(
                directory.resolve("master-key.json"), "", true, false)) {
            String cursor = codec.encode("items", "run_1\0alice");
            assertEquals("run_1\0alice", codec.decode("items", cursor));

            int last = cursor.length() - 1;
            int canonicalIndex = BASE64_URL.indexOf(cursor.charAt(last));
            String nonCanonical = cursor.substring(0, last)
                    + BASE64_URL.charAt(canonicalIndex + 1);
            assertThrows(IllegalArgumentException.class,
                    () -> codec.decode("items", nonCanonical));
        }
    }
}

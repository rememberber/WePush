package com.fangxuele.tool.push.ai;

import com.google.gson.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;

final class AiJson {
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setStrictness(Strictness.STRICT).create();

    private AiJson() { }

    static JsonElement parse(String value) { return GSON.fromJson(value, JsonElement.class); }

    static JsonObject object(Object... pairs) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2) {
            result.add((String) pairs[i], GSON.toJsonTree(pairs[i + 1]));
        }
        return result;
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static void privateDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        permissions(directory, "rwx------");
    }

    static void permissions(Path path, String mode) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
        }
    }

    /** Write completely before replacing; a persisted request must precede any delivery. */
    static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), ".wepush-", ".tmp");
        try {
            permissions(temporary, "rw-------");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Set;

public final class LocalFileArtifactStore implements ArtifactStore {
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("uuuu").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC);
    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private final Path root;
    private final String environment;

    public LocalFileArtifactStore(Path root, String environment) {
        this.root = root.toAbsolutePath().normalize();
        this.environment = segment(environment, "environment");
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new IllegalStateException("artifact root cannot be created", exception);
        }
    }

    @Override
    public ObjectPlan plan(WorkspaceId workspaceId, String artifactId, String type, Instant createdAt) {
        String safeWorkspace = segment(workspaceId.value(), "workspaceId");
        String safeArtifact = segment(artifactId, "artifactId");
        String safeType = segment(type.toLowerCase(java.util.Locale.ROOT).replace('_', '-'), "type");
        String location = String.join("/", environment, safeWorkspace, safeType,
                YEAR.format(createdAt), MONTH.format(createdAt), safeArtifact);
        resolve(location);
        return new ObjectPlan("LOCAL_FILE", location);
    }

    @Override
    public StoredObject write(ObjectPlan plan, ContentWriter writer) throws IOException {
        if (!"LOCAL_FILE".equals(plan.backend())) {
            throw new IllegalArgumentException("artifact plan uses another backend");
        }
        Path target = resolve(plan.location());
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".wepush-artifact-", ".tmp");
        secure(temporary);
        MessageDigest digest = sha256();
        long size;
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                OutputStream digestOutput = new DigestOutputStream(Channels.newOutputStream(channel), digest);
                BufferedOutputStream buffered = new BufferedOutputStream(digestOutput, 64 * 1024);
                writer.write(buffered);
                buffered.flush();
                channel.force(true);
                size = channel.size();
            }
            secure(temporary);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            secure(target);
            syncDirectory(parent);
            return new StoredObject(size, HexFormat.of().formatHex(digest.digest()));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public InputStream open(String location, long offset, long length) throws IOException {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("artifact range must be non-negative");
        }
        InputStream input = Files.newInputStream(resolve(location));
        try {
            input.skipNBytes(offset);
            return new BoundedInputStream(input, length);
        } catch (IOException | RuntimeException exception) {
            input.close();
            throw exception;
        }
    }

    @Override
    public void delete(String location) throws IOException {
        Files.deleteIfExists(resolve(location));
    }

    private Path resolve(String location) {
        if (location == null || location.startsWith("/") || location.contains("\\")) {
            throw new IllegalArgumentException("artifact location is invalid");
        }
        Path resolved = root.resolve(location).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("artifact location escapes the configured root");
        }
        return resolved;
    }

    private static String segment(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(label + " contains unsupported characters");
        }
        return value;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void secure(Path path) throws IOException {
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, OWNER_ONLY);
        }
    }

    private static void syncDirectory(Path directory) {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException ignored) {
            // The object file itself is already durable; some platforms cannot fsync directories.
        }
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private long remaining;

        private BoundedInputStream(InputStream input, long remaining) {
            super(input);
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0) return -1;
            int value = super.read();
            if (value >= 0) remaining--;
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining == 0) return -1;
            int read = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) remaining -= read;
            return read;
        }
    }
}

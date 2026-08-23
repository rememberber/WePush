package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FileAgentCompletionOutbox implements AgentCompletionOutbox {
    private static final int MAGIC = 0x57455043; // WEPC
    private static final int VERSION = 1;
    private static final int MAXIMUM_COMPLETIONS = 10_000;
    private static final int MAXIMUM_SUMMARY_BYTES = 1024 * 1024;
    private final Path path;
    private final Map<LeaseFence, PendingCompletion> pending = new LinkedHashMap<>();

    public FileAgentCompletionOutbox(Path path) {
        if (path == null) throw new IllegalArgumentException("completion Outbox path is required");
        this.path = path.toAbsolutePath().normalize();
        load();
    }

    @Override
    public synchronized void put(LeaseFence fence, byte[] summary, List<String> artifactReferences) {
        if (summary.length > MAXIMUM_SUMMARY_BYTES) {
            throw new IllegalArgumentException("Agent completion summary is too large");
        }
        if (pending.size() >= MAXIMUM_COMPLETIONS && !pending.containsKey(fence)) {
            throw new IllegalStateException("Agent completion Outbox is full");
        }
        if (pending.putIfAbsent(fence, new PendingCompletion(fence, summary, artifactReferences)) == null) {
            persist();
        }
    }

    @Override
    public synchronized List<PendingCompletion> pending() {
        return List.copyOf(pending.values());
    }

    @Override
    public synchronized void acknowledge(LeaseFence fence) {
        if (pending.remove(fence) != null) persist();
    }

    private void load() {
        if (!Files.exists(path)) return;
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported completion Outbox format");
            }
            int count = input.readInt();
            if (count < 0 || count > MAXIMUM_COMPLETIONS) throw new IOException("invalid completion count");
            for (int index = 0; index < count; index++) {
                LeaseFence fence = new LeaseFence(input.readUTF(), input.readUTF(), input.readLong(),
                        input.readUTF());
                int length = input.readInt();
                if (length < 1 || length > MAXIMUM_SUMMARY_BYTES) throw new IOException("invalid summary size");
                byte[] summary = input.readNBytes(length);
                if (summary.length != length) throw new EOFException();
                int artifactCount = input.readInt();
                if (artifactCount < 0 || artifactCount > 10_000) throw new IOException("invalid artifact count");
                java.util.ArrayList<String> artifacts = new java.util.ArrayList<>(artifactCount);
                for (int artifact = 0; artifact < artifactCount; artifact++) artifacts.add(input.readUTF());
                pending.put(fence, new PendingCompletion(fence, summary, artifacts));
            }
            if (input.read() != -1) throw new IOException("trailing completion Outbox data");
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load Agent completion Outbox: " + path, exception);
        }
    }

    private void persist() {
        Path parent = path.getParent();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            if (parent != null) Files.createDirectories(parent);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(temporary)))) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeInt(pending.size());
                for (PendingCompletion value : pending.values()) {
                    output.writeUTF(value.fence().leaseId());
                    output.writeUTF(value.fence().runId());
                    output.writeLong(value.fence().epoch());
                    output.writeUTF(value.fence().fencingToken());
                    byte[] summary = value.summary();
                    output.writeInt(summary.length);
                    output.write(summary);
                    output.writeInt(value.artifactReferences().size());
                    for (String artifact : value.artifactReferences()) output.writeUTF(artifact);
                }
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot save Agent completion Outbox: " + path, exception);
        }
    }
}

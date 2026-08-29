package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded, atomically-replaced local Event Outbox. It intentionally contains
 * only already-redacted Agent reports and never execution snapshots or Secrets.
 */
public final class FileAgentEventOutbox implements AgentEventOutbox {
    public static final long DEFAULT_MAXIMUM_BYTES = 64L * 1024 * 1024;
    private static final int MAGIC = 0x5745504f; // WEPO
    private static final int VERSION = 1;
    private static final int MAXIMUM_EVENT_BYTES = 8 * 1024 * 1024;
    private static final int MAXIMUM_BATCHES = 1_000_000;

    private final Path path;
    private final long maximumBytes;
    private final List<PendingBatch> batches = new ArrayList<>();
    private final Map<String, Long> nextSequences = new HashMap<>();
    private long sizeBytes;

    public FileAgentEventOutbox(Path path) {
        this(path, DEFAULT_MAXIMUM_BYTES);
    }

    public FileAgentEventOutbox(Path path, long maximumBytes) {
        if (path == null || maximumBytes < 1) {
            throw new IllegalArgumentException("Event Outbox path and positive limit are required");
        }
        this.path = path.toAbsolutePath().normalize();
        this.maximumBytes = maximumBytes;
        load();
    }

    @Override
    public synchronized PendingBatch append(LeaseFence fence, List<byte[]> events) {
        long first = nextSequences.getOrDefault(fence.leaseId(), 1L);
        PendingBatch batch = new PendingBatch(fence, first, events);
        long added = encodedPayloadBytes(batch);
        if (added > maximumBytes - sizeBytes) {
            throw new EventOutboxFullException(maximumBytes, sizeBytes, added);
        }
        List<PendingBatch> updatedBatches = new ArrayList<>(batches);
        updatedBatches.add(batch);
        Map<String, Long> updatedSequences = new HashMap<>(nextSequences);
        updatedSequences.put(fence.leaseId(), batch.lastEventSequence() + 1L);
        persist(updatedBatches, updatedSequences);
        batches.add(batch);
        nextSequences.put(fence.leaseId(), batch.lastEventSequence() + 1L);
        sizeBytes += added;
        return batch;
    }

    @Override
    public synchronized List<PendingBatch> pending() {
        return List.copyOf(batches);
    }

    @Override
    public synchronized void acknowledge(AgentFrames.EventAck acknowledgement) {
        LeaseFence fence = acknowledgement.fence();
        List<PendingBatch> updatedBatches = new ArrayList<>(batches);
        boolean changed = updatedBatches.removeIf(batch -> batch.fence().equals(fence)
                && batch.lastEventSequence() <= acknowledgement.lastEventSequence());
        if (changed) {
            persist(updatedBatches, nextSequences);
            batches.clear();
            batches.addAll(updatedBatches);
            sizeBytes = batches.stream().mapToLong(FileAgentEventOutbox::encodedPayloadBytes).sum();
        }
    }

    @Override
    public synchronized long sizeBytes() {
        return sizeBytes;
    }

    private void load() {
        if (!Files.exists(path)) return;
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(
                Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported Event Outbox format");
            }
            int sequenceCount = boundedCount(input.readInt(), "sequence cursor");
            for (int index = 0; index < sequenceCount; index++) {
                String leaseId = input.readUTF();
                long next = input.readLong();
                if (leaseId.isBlank() || next < 1) throw new IOException("invalid sequence cursor");
                nextSequences.put(leaseId, next);
            }
            int batchCount = boundedCount(input.readInt(), "batch");
            for (int index = 0; index < batchCount; index++) {
                LeaseFence fence = new LeaseFence(input.readUTF(), input.readUTF(), input.readLong(),
                        input.readUTF());
                long first = input.readLong();
                int eventCount = boundedCount(input.readInt(), "event");
                if (eventCount == 0) throw new IOException("empty Event Outbox batch");
                List<byte[]> events = new ArrayList<>(eventCount);
                for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
                    int length = input.readInt();
                    if (length < 1 || length > MAXIMUM_EVENT_BYTES) {
                        throw new IOException("invalid Event Outbox event length");
                    }
                    events.add(input.readNBytes(length));
                    if (events.get(eventIndex).length != length) throw new EOFException();
                }
                PendingBatch batch = new PendingBatch(fence, first, events);
                batches.add(batch);
                sizeBytes += encodedPayloadBytes(batch);
            }
            if (input.read() != -1) throw new IOException("trailing Event Outbox data");
            if (sizeBytes > maximumBytes) throw new IOException("Event Outbox exceeds configured limit");
            batches.sort(Comparator.comparing(PendingBatch::fence,
                    Comparator.comparing(LeaseFence::leaseId)).thenComparingLong(PendingBatch::firstEventSequence));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load Agent Event Outbox: " + path, exception);
        }
    }

    private void persist(List<PendingBatch> persistedBatches, Map<String, Long> persistedSequences) {
        Path parent = path.getParent();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            if (parent != null) Files.createDirectories(parent);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(temporary)))) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeInt(persistedSequences.size());
                for (Map.Entry<String, Long> entry : persistedSequences.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).toList()) {
                    output.writeUTF(entry.getKey());
                    output.writeLong(entry.getValue());
                }
                output.writeInt(persistedBatches.size());
                for (PendingBatch batch : persistedBatches) {
                    output.writeUTF(batch.fence().leaseId());
                    output.writeUTF(batch.fence().runId());
                    output.writeLong(batch.fence().epoch());
                    output.writeUTF(batch.fence().fencingToken());
                    output.writeLong(batch.firstEventSequence());
                    List<byte[]> events = batch.events();
                    output.writeInt(events.size());
                    for (byte[] event : events) {
                        output.writeInt(event.length);
                        output.write(event);
                    }
                }
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Preserve the original failure; a non-empty or locked temp path is diagnostic evidence.
            }
            throw new IllegalStateException("Cannot save Agent Event Outbox: " + path, exception);
        }
    }

    private static int boundedCount(int value, String name) throws IOException {
        if (value < 0 || value > MAXIMUM_BATCHES) throw new IOException("invalid " + name + " count");
        return value;
    }

    private static long encodedPayloadBytes(PendingBatch batch) {
        return batch.events().stream().mapToLong(value -> Integer.BYTES + value.length).sum();
    }

    public static final class EventOutboxFullException extends IllegalStateException {
        EventOutboxFullException(long maximumBytes, long currentBytes, long requestedBytes) {
            super("Agent Event Outbox is full: limit=" + maximumBytes + ", current="
                    + currentBytes + ", requested=" + requestedBytes);
        }
    }
}

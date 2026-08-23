package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class FileAgentJournal implements AgentJournal {
    private final Path path;

    public FileAgentJournal(Path path) {
        if (path == null) throw new IllegalArgumentException("agent journal path is required");
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public synchronized AgentJournalState load() {
        if (!Files.exists(path)) return AgentJournalState.empty();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            long lastAgentSequence = number(properties, "lastAgentSequence");
            long lastServiceSequence = number(properties, "lastServiceSequence");
            int count = Math.toIntExact(number(properties, "leaseCount"));
            Map<String, AgentJournalState.PersistedLease> leases = new HashMap<>();
            for (int index = 0; index < count; index++) {
                String prefix = "lease." + index + ".";
                LeaseFence fence = new LeaseFence(
                        required(properties, prefix + "id"),
                        required(properties, prefix + "runId"),
                        number(properties, prefix + "epoch"),
                        required(properties, prefix + "fencingToken"));
                AgentJournalState.PersistedLease lease = new AgentJournalState.PersistedLease(
                        fence,
                        Instant.parse(required(properties, prefix + "expiresAt")),
                        LeaseState.valueOf(required(properties, prefix + "state")),
                        properties.getProperty(prefix + "executionSpecSha256", ""),
                        properties.getProperty(prefix + "audienceSha256", ""),
                        Long.parseLong(properties.getProperty(prefix + "totalRecipients", "-1")),
                        optionalInstant(properties.getProperty(prefix + "executionStartedAt")));
                leases.put(fence.leaseId(), lease);
            }
            return new AgentJournalState(lastAgentSequence, lastServiceSequence, leases);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load Agent journal: " + path, exception);
        }
    }

    @Override
    public synchronized void save(AgentJournalState state) {
        Properties properties = new Properties();
        properties.setProperty("lastAgentSequence", Long.toString(state.lastAgentSequence()));
        properties.setProperty("lastServiceSequence", Long.toString(state.lastServiceSequence()));
        properties.setProperty("leaseCount", Integer.toString(state.leases().size()));
        int index = 0;
        for (AgentJournalState.PersistedLease lease : state.leases().values()) {
            String prefix = "lease." + index++ + ".";
            properties.setProperty(prefix + "id", lease.fence().leaseId());
            properties.setProperty(prefix + "runId", lease.fence().runId());
            properties.setProperty(prefix + "epoch", Long.toString(lease.fence().epoch()));
            properties.setProperty(prefix + "fencingToken", lease.fence().fencingToken());
            properties.setProperty(prefix + "expiresAt", lease.expiresAt().toString());
            properties.setProperty(prefix + "state", lease.state().name());
            properties.setProperty(prefix + "executionSpecSha256", lease.executionSpecSha256());
            properties.setProperty(prefix + "audienceSha256", lease.audienceSha256());
            properties.setProperty(prefix + "totalRecipients", Long.toString(lease.totalRecipients()));
            if (lease.executionStartedAt() != null) {
                properties.setProperty(prefix + "executionStartedAt", lease.executionStartedAt().toString());
            }
        }
        Path parent = path.getParent();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "WePush Next Agent journal");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot save Agent journal: " + path, exception);
        }
    }

    private static long number(Properties properties, String name) {
        String value = properties.getProperty(name, "0");
        long parsed = Long.parseLong(value);
        if (parsed < 0) throw new IllegalArgumentException(name + " must not be negative");
        return parsed;
    }

    private static String required(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is missing");
        return value;
    }

    private static Instant optionalInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}

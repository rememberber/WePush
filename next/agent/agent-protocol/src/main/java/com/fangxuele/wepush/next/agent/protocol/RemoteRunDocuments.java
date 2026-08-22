package com.fangxuele.wepush.next.agent.protocol;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * JSON documents exchanged outside the gRPC control stream. Keeping these records in the
 * framework-free protocol module lets Service and Agent evolve independently from Core internals.
 */
public final class RemoteRunDocuments {
    private RemoteRunDocuments() {
    }

    public record ExecutionSpec(
            String runId,
            String workspaceId,
            String providerId,
            String providerVersion,
            String accountConfigurationJson,
            String messageContentJson,
            String policiesJson,
            Map<String, String> attributes,
            boolean dryRun,
            Instant createdAt
    ) {
        public ExecutionSpec {
            requireText(runId, "runId");
            requireText(workspaceId, "workspaceId");
            requireText(providerId, "providerId");
            requireText(providerVersion, "providerVersion");
            requireText(accountConfigurationJson, "accountConfigurationJson");
            requireText(messageContentJson, "messageContentJson");
            requireText(policiesJson, "policiesJson");
            if (createdAt == null) throw new IllegalArgumentException("createdAt is required");
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }

    public record Audience(List<Recipient> recipients) {
        public Audience {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }
    }

    public record Recipient(long sequence, String itemId, String fieldsJson) {
        public Recipient {
            if (sequence < 0) throw new IllegalArgumentException("recipient sequence must be non-negative");
            requireText(itemId, "itemId");
            requireText(fieldsJson, "fieldsJson");
        }
    }

    /** One ordered Agent outbox entry. Exactly one payload family is populated. */
    public record Report(String kind, Event event, List<ItemResult> results) {
        public Report {
            requireText(kind, "kind");
            results = results == null ? List.of() : List.copyOf(results);
            if (("EVENT".equals(kind) && (event == null || !results.isEmpty()))
                    || ("RESULTS".equals(kind) && (event != null || results.isEmpty()))
                    || (!"EVENT".equals(kind) && !"RESULTS".equals(kind))) {
                throw new IllegalArgumentException("remote report payload does not match its kind");
            }
        }

        public static Report event(Event event) {
            return new Report("EVENT", event, List.of());
        }

        public static Report results(List<ItemResult> results) {
            return new Report("RESULTS", null, results);
        }
    }

    public record Event(String runId, long sequence, String type, Instant occurredAt,
                        Map<String, String> data) {
        public Event {
            requireText(runId, "runId");
            requireText(type, "type");
            if (sequence < 1 || occurredAt == null) {
                throw new IllegalArgumentException("event sequence and occurredAt are required");
            }
            data = data == null ? Map.of() : Map.copyOf(data);
        }
    }

    public record ItemResult(
            String runId,
            String itemId,
            int attempts,
            String state,
            String providerCode,
            String diagnostic,
            String externalRequestId,
            Instant completedAt,
            Map<String, String> metadata
    ) {
        public ItemResult {
            requireText(runId, "runId");
            requireText(itemId, "itemId");
            requireText(state, "state");
            if (attempts < 0 || completedAt == null) {
                throw new IllegalArgumentException("result attempts and completedAt are invalid");
            }
            providerCode = providerCode == null ? "" : providerCode;
            diagnostic = diagnostic == null ? "" : diagnostic;
            externalRequestId = externalRequestId == null ? "" : externalRequestId;
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    public record Summary(
            String runId,
            String finalState,
            long total,
            long succeeded,
            long failed,
            long unknown,
            long unsent,
            long skipped,
            long retried,
            Instant startedAt,
            Instant endedAt
    ) {
        public Summary {
            requireText(runId, "runId");
            requireText(finalState, "finalState");
            if (total < 0 || succeeded < 0 || failed < 0 || unknown < 0 || unsent < 0
                    || skipped < 0 || retried < 0 || startedAt == null || endedAt == null
                    || endedAt.isBefore(startedAt)
                    || total != succeeded + failed + unknown + unsent + skipped) {
                throw new IllegalArgumentException("remote summary invariants are invalid");
            }
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}

package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.MessageRevision;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MessageApplicationService {
    private final WorkspaceRepository workspaces;
    private final MessageRepository messages;
    private final ProviderRegistry providers;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public MessageApplicationService(WorkspaceRepository workspaces, MessageRepository messages,
                                     ProviderRegistry providers, JsonCodec json, ResourceIdGenerator ids,
                                     TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.messages = messages;
        this.providers = providers;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public MessageDefinition create(WorkspaceId workspaceId, CreateMessage command) {
        return transactions.required(() -> {
            ApplicationSupport.requireWorkspace(workspaces, workspaceId);
            ProviderRef ref = new ProviderRef(ApplicationSupport.text(command.providerId(), "providerId"),
                    ApplicationSupport.text(command.providerVersion(), "providerVersion"));
            ProviderFactory provider = ApplicationSupport.requireProvider(providers, ref);
            JsonDocument content = json.canonicalize(command.content());
            ApplicationSupport.requireValid(provider.validateMessage(
                    ApplicationSupport.config(content, provider.descriptor().messageSchema())));
            Instant now = clock.instant();
            MessageDefinition message = new MessageDefinition(ids.next("msg"), workspaceId,
                    ApplicationSupport.text(command.name(), "name"), ref, 1,
                    provider.descriptor().messageSchema().schemaVersion(), content,
                    ApplicationSupport.sha256(content.value()), MessageDefinition.Status.ACTIVE,
                    now, now, 0);
            messages.create(message);
            return message;
        });
    }

    public MessageDefinition get(WorkspaceId workspaceId, String messageId) {
        return messages.findById(workspaceId, messageId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "MESSAGE_NOT_FOUND",
                        "Message was not found: " + messageId));
    }

    public List<MessageDefinition> list(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return messages.list(workspaceId);
    }

    public MessageDefinition update(WorkspaceId workspaceId, String messageId, UpdateMessage command) {
        return transactions.required(() -> {
            MessageDefinition current = get(workspaceId, messageId);
            String name = command.name() == null ? current.name() : ApplicationSupport.text(command.name(), "name");
            MessageDefinition.Status status = command.status() == null ? current.status()
                    : MessageDefinition.Status.valueOf(command.status().toUpperCase());
            if (command.content() == null) {
                MessageDefinition updated = new MessageDefinition(current.id(), workspaceId, name,
                        current.provider(), current.revision(), current.schemaVersion(), current.content(),
                        current.contentHash(), status, current.createdAt(), clock.instant(), current.version() + 1);
                if (!messages.updateMetadata(updated, current.version())) conflict(messageId);
                return updated;
            }
            ProviderFactory provider = ApplicationSupport.requireProvider(providers, current.provider());
            JsonDocument content = json.canonicalize(command.content());
            ApplicationSupport.requireValid(provider.validateMessage(
                    ApplicationSupport.config(content, provider.descriptor().messageSchema())));
            MessageDefinition updated = new MessageDefinition(current.id(), workspaceId, name,
                    current.provider(), current.revision() + 1, provider.descriptor().messageSchema().schemaVersion(),
                    content, ApplicationSupport.sha256(content.value()), status, current.createdAt(),
                    clock.instant(), current.version() + 1);
            if (!messages.createRevision(updated, current.version())) conflict(messageId);
            return updated;
        });
    }

    public MessageDefinition copy(WorkspaceId workspaceId, String messageId, String name) {
        MessageDefinition source = get(workspaceId, messageId);
        return create(workspaceId, new CreateMessage(ApplicationSupport.text(name, "name"),
                source.provider().providerId(), source.provider().implementationVersion(),
                json.read(source.content(), Object.class)));
    }

    public RevisionPage revisions(WorkspaceId workspaceId, String messageId, int beforeRevision, int limit) {
        get(workspaceId, messageId);
        if (limit < 1 || limit > 100) throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST,
                "INVALID_PAGE_LIMIT", "Revision page limit must be between 1 and 100");
        List<MessageRevision> loaded = messages.revisions(workspaceId, messageId,
                beforeRevision <= 0 ? Integer.MAX_VALUE : beforeRevision, limit + 1);
        boolean more = loaded.size() > limit;
        List<MessageRevision> items = more ? List.copyOf(loaded.subList(0, limit)) : List.copyOf(loaded);
        return new RevisionPage(items, more && !items.isEmpty() ? items.getLast().revision() : null, more);
    }

    public MessageDiff diff(WorkspaceId workspaceId, String messageId, int from, int to) {
        MessageRevision before = messages.findRevision(workspaceId, messageId, from).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "MESSAGE_REVISION_NOT_FOUND",
                        "Message revision was not found: " + from));
        MessageRevision after = messages.findRevision(workspaceId, messageId, to).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "MESSAGE_REVISION_NOT_FOUND",
                        "Message revision was not found: " + to));
        Map<?, ?> left = json.read(before.content(), Map.class);
        Map<?, ?> right = json.read(after.content(), Map.class);
        List<String> changed = new java.util.ArrayList<>();
        java.util.Set<Object> keys = new java.util.TreeSet<>(java.util.Comparator.comparing(String::valueOf));
        keys.addAll(left.keySet()); keys.addAll(right.keySet());
        for (Object key : keys) if (!java.util.Objects.equals(left.get(key), right.get(key))) changed.add("/" + key);
        return new MessageDiff(before, after, List.copyOf(changed));
    }

    private static void conflict(String id) {
        throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RESOURCE_VERSION_CONFLICT",
                "Message was changed concurrently: " + id);
    }

    public record CreateMessage(String name, String providerId, String providerVersion, Object content) {
    }

    public record UpdateMessage(String name, Object content, String status) { }
    public record RevisionPage(List<MessageRevision> items, Integer nextBeforeRevision, boolean hasMore) { }
    public record MessageDiff(MessageRevision from, MessageRevision to, List<String> changedPaths) { }
}

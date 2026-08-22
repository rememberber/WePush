package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

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

    public record CreateMessage(String name, String providerId, String providerVersion, Object content) {
    }
}

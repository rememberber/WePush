package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRecipient;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AudienceApplicationService {
    private final WorkspaceRepository workspaces;
    private final AudienceRepository audiences;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public AudienceApplicationService(WorkspaceRepository workspaces, AudienceRepository audiences,
                                      JsonCodec json, ResourceIdGenerator ids,
                                      TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.audiences = audiences;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public AudienceDefinition create(WorkspaceId workspaceId, CreateAudience command) {
        return transactions.required(() -> {
            ApplicationSupport.requireWorkspace(workspaces, workspaceId);
            if (command.recipients() == null) {
                throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "FIELD_REQUIRED",
                        "recipients must be supplied");
            }
            List<AudienceRecipient> recipients = canonicalRecipients(command.recipients());
            JsonDocument content = json.canonicalize(recipients.stream()
                    .map(recipient -> new RecipientInput(recipient.itemId(),
                            json.read(recipient.fields(), Object.class))).toList());
            Instant now = clock.instant();
            AudienceDefinition audience = new AudienceDefinition(ids.next("aud"), workspaceId,
                    ApplicationSupport.text(command.name(), "name"), ids.next("audsnap"), 1,
                    recipients.size(), ApplicationSupport.sha256(content.value()),
                    AudienceDefinition.Status.ACTIVE, now, now, 0);
            audiences.create(audience, recipients);
            return audience;
        });
    }

    public AudienceDefinition get(WorkspaceId workspaceId, String audienceId) {
        return audiences.findById(workspaceId, audienceId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "AUDIENCE_NOT_FOUND",
                        "Audience was not found: " + audienceId));
    }

    public List<AudienceDefinition> list(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return audiences.list(workspaceId);
    }

    public AudienceDefinition update(WorkspaceId workspaceId, String audienceId, UpdateAudience command) {
        return transactions.required(() -> {
            AudienceDefinition current = get(workspaceId, audienceId);
            String name = command.name() == null ? current.name() : ApplicationSupport.text(command.name(), "name");
            AudienceDefinition.Status status = command.status() == null ? current.status()
                    : AudienceDefinition.Status.valueOf(command.status().toUpperCase());
            if (command.recipients() == null) {
                AudienceDefinition updated = new AudienceDefinition(current.id(), workspaceId, name,
                        current.snapshotId(), current.revision(), current.recordCount(), current.contentHash(),
                        status, current.createdAt(), clock.instant(), current.version() + 1);
                if (!audiences.updateMetadata(updated, current.version())) conflict(audienceId);
                return updated;
            }
            List<AudienceRecipient> recipients = canonicalRecipients(command.recipients());
            JsonDocument content = json.canonicalize(recipients.stream().map(recipient ->
                    new RecipientInput(recipient.itemId(), json.read(recipient.fields(), Object.class))).toList());
            AudienceDefinition updated = new AudienceDefinition(current.id(), workspaceId, name,
                    ids.next("audsnap"), current.revision() + 1, recipients.size(),
                    ApplicationSupport.sha256(content.value()), status, current.createdAt(),
                    clock.instant(), current.version() + 1);
            if (!audiences.createRevision(updated, recipients, current.version())) conflict(audienceId);
            return updated;
        });
    }

    private static void conflict(String id) {
        throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RESOURCE_VERSION_CONFLICT",
                "Audience was changed concurrently: " + id);
    }

    private List<AudienceRecipient> canonicalRecipients(List<RecipientInput> input) {
        List<AudienceRecipient> recipients = new ArrayList<>(input.size());
        Set<String> itemIds = new HashSet<>();
        for (int index = 0; index < input.size(); index++) {
            RecipientInput recipient = input.get(index);
            if (recipient == null || recipient.fields() == null) {
                throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "RECIPIENT_INVALID",
                        "Each recipient must contain fields");
            }
            JsonDocument fields = json.canonicalize(recipient.fields());
            json.read(fields, java.util.Map.class);
            String itemId = recipient.itemId();
            if (itemId == null || itemId.isBlank()) {
                itemId = "item_" + index + "_" + ApplicationSupport.sha256(fields.value()).substring(0, 12);
            }
            itemId = itemId.trim();
            if (!itemIds.add(itemId)) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "DUPLICATE_RECIPIENT_ITEM_ID",
                        "Duplicate recipient itemId: " + itemId);
            }
            recipients.add(new AudienceRecipient(index, itemId, fields));
        }
        return List.copyOf(recipients);
    }

    public record CreateAudience(String name, List<RecipientInput> recipients) {
    }

    public record RecipientInput(String itemId, Object fields) {
    }

    public record UpdateAudience(String name, List<RecipientInput> recipients, String status) { }
}

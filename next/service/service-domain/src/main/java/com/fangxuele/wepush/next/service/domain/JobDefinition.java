package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record JobDefinition(
        String id,
        WorkspaceId workspaceId,
        String name,
        String accountId,
        String messageId,
        String audienceId,
        JsonDocument policies,
        boolean enabled,
        boolean archived,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public JobDefinition {
        id = DomainChecks.text(id, "job id");
        name = DomainChecks.text(name, "job name");
        accountId = DomainChecks.text(accountId, "job account id");
        messageId = DomainChecks.text(messageId, "job message id");
        audienceId = DomainChecks.text(audienceId, "job audience id");
        if (workspaceId == null || policies == null || createdAt == null || updatedAt == null
                || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("job definition is incomplete");
        }
        DomainChecks.nonNegative(version, "job version");
        if (archived && enabled) {
            throw new IllegalArgumentException("archived job cannot be enabled");
        }
    }

    public String status() {
        return archived ? "ARCHIVED" : enabled ? "ACTIVE" : "DISABLED";
    }
}

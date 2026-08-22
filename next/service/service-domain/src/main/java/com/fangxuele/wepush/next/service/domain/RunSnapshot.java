package com.fangxuele.wepush.next.service.domain;

import com.fangxuele.wepush.next.core.api.ProviderRef;

public record RunSnapshot(
        String id,
        String runId,
        WorkspaceId workspaceId,
        ProviderRef provider,
        JsonDocument accountConfiguration,
        JsonDocument messageContent,
        JsonDocument policies,
        String audienceSnapshotId,
        String contentHash
) {
    public RunSnapshot {
        id = DomainChecks.text(id, "run snapshot id");
        runId = DomainChecks.text(runId, "snapshot run id");
        audienceSnapshotId = DomainChecks.text(audienceSnapshotId, "snapshot audience id");
        contentHash = DomainChecks.text(contentHash, "run snapshot hash");
        if (workspaceId == null || provider == null || accountConfiguration == null
                || messageContent == null || policies == null) {
            throw new IllegalArgumentException("run snapshot is incomplete");
        }
    }
}

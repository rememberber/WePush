package com.fangxuele.wepush.next.service.domain;

import java.util.List;
import java.util.Optional;

public interface AudienceRepository {
    void create(AudienceDefinition audience, List<AudienceRecipient> recipients);

    Optional<AudienceDefinition> findById(WorkspaceId workspaceId, String audienceId);

    List<AudienceDefinition> list(WorkspaceId workspaceId);

    List<AudienceRecipient> recipients(WorkspaceId workspaceId, String snapshotId);
}

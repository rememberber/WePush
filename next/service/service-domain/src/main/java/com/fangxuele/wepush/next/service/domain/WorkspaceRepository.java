package com.fangxuele.wepush.next.service.domain;

import java.util.Optional;

public interface WorkspaceRepository {
    Optional<Workspace> findById(WorkspaceId workspaceId);

    void save(Workspace workspace, long expectedVersion);
}

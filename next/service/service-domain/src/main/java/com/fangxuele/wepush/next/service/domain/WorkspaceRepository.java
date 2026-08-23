package com.fangxuele.wepush.next.service.domain;

import java.util.Optional;
import java.util.List;

public interface WorkspaceRepository {
    Optional<Workspace> findById(WorkspaceId workspaceId);

    List<Workspace> list();

    void create(Workspace workspace);

    void save(Workspace workspace, long expectedVersion);
}
